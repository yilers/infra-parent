package io.github.yilers.upm.permission;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import io.github.yilers.upm.handler.UserHandler;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.permission.DataPermission;
import io.github.yilers.web.permission.DataPermissionContext;
import io.github.yilers.web.permission.DataPermissionContextHolder;
import io.github.yilers.web.permission.DataScope;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
public class CustomDataPermissionHandler implements MultiDataPermissionHandler {

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer PARAM_DISCOVERER = new DefaultParameterNameDiscoverer();
    private BeanResolver beanResolver;

    private static final ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Method> MAPPED_STATEMENT_ID_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Method, DataPermission> PERMISSION_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Method, DataScope> DATA_SCOPE_CACHE = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        List<DataPermissionContext> ctxList = DataPermissionContextHolder.get();
        if (CollUtil.isNotEmpty(ctxList)) {
            log.debug("Using ThreadLocal DataPermissionContext for {}", table.getName());
//            DataPermissionContextHolder.clear();
            return buildExpressionFromContextList(ctxList, table);
        }
        Method method = getMethodFromMappedStatement(mappedStatementId);
        if (method == null) {
            return null;
        }

        List<Expression> expressions = new ArrayList<>();

        // 单个 DataPermission 注解
        DataPermission dataPermission = getCachedPermission(method);
        if (dataPermission != null) {
            Expression expr = parseSinglePermission(table.getName(), getAlias(table), dataPermission, method);
            if (expr != null) {
                expressions.add(expr);
            }
        }

        // DataScope 多个注解
        DataScope dataScope = getCachedDataScope(method);
        if (dataScope != null) {
            for (DataPermission permission : dataScope.value()) {
                Expression expr = parseSinglePermission(table.getName(), getAlias(table), permission, method);
                if (expr != null) {
                    expressions.add(expr);
                }
            }
        }

        // 最终返回拼接的 AndExpression
        return CollUtil.isNotEmpty(expressions) ?
                expressions.stream().reduce(AndExpression::new).orElse(null) :
                null;
    }

    private Expression buildExpressionFromContextList(List<DataPermissionContext> contextList, Table table) throws Exception {
        List<Expression> expressions = new ArrayList<>();
        String alias = getAlias(table);

        for (DataPermissionContext ctx : contextList) {
            String tableName = ctx.getTableName();
            String column = ctx.getColumnName();
            String columnValue = ctx.getColumnValue();
            String deptField = ctx.getDeptField();
            String userField = ctx.getUserField();

            // 判断 tableName 是否匹配当前表（可以为空）
            if (StrUtil.isNotBlank(tableName) && !tableName.equals(table.getName())) {
                continue;
            }
            DataPermissionContextHolder.clear();

            if (StrUtil.isNotBlank(columnValue)) {
                // 有 columnValue，直接拼接
                expressions.add(CCJSqlParserUtil.parseCondExpression(columnValue));
            } else {
                // 没有 columnValue，走默认逻辑
                String condition = buildDefaultPermissionSql(alias, deptField, userField);
                expressions.add(CCJSqlParserUtil.parseCondExpression(condition));
            }
        }

        return expressions.stream().reduce(AndExpression::new).orElse(null);
    }

    private Method getMethodFromMappedStatement(String mappedStatementId) {
        return MAPPED_STATEMENT_ID_METHOD_CACHE.computeIfAbsent(mappedStatementId, id -> {
            try {
                String classPath = id.substring(0, id.lastIndexOf('.'));
                String methodName = id.substring(id.lastIndexOf('.') + 1);

                Class<?> clazz = CLASS_CACHE.computeIfAbsent(classPath, key -> {
                    try {
                        return Class.forName(key);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                for (Method m : clazz.getMethods()) {
                    if (m.getName().equals(methodName)) {
                        return m;
                    }
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private DataPermission getCachedPermission(Method method) {
        return PERMISSION_CACHE.computeIfAbsent(method, m -> {
            DataPermission dp = m.getAnnotation(DataPermission.class);
            if (dp == null) {
                dp = m.getDeclaringClass().getAnnotation(DataPermission.class);
            }
            return dp;
        });
    }

    private DataScope getCachedDataScope(Method method) {
        return DATA_SCOPE_CACHE.computeIfAbsent(method, m -> {
            DataScope ds = m.getAnnotation(DataScope.class);
            if (ds == null) {
                ds = m.getDeclaringClass().getAnnotation(DataScope.class);
            }
            return ds;
        });
    }

    private String getAlias(Table table) {
        return Optional.ofNullable(table.getAlias()).map(Alias::getName).orElse("");
    }

    private Expression parseSinglePermission(String table, String alias, DataPermission permission, Method method) throws Exception {
        String condition = buildPermissionSqlFromAnnotation(table, alias, permission, method);
        return StrUtil.isNotBlank(condition) ? CCJSqlParserUtil.parseCondExpression(condition) : null;
    }

    private String buildPermissionSqlFromAnnotation(String table, String alias, DataPermission anno, Method method) {
        String annoTable = anno.tableName();
        String column = anno.columnName();
        String spel = anno.columnValue();
        String deptField = anno.deptField();
        String userField = anno.userField();

        // 先判断 tableName 是否匹配当前表
        if (StrUtil.isNotBlank(annoTable) && !annoTable.equals(table)) {
            return "";
        }

        // 优先判断是否有 column + SpEL
        if (StrUtil.isNotBlank(column) && StrUtil.isNotBlank(spel)) {
            return buildSpelPermissionSql(alias, column, spel, method);
        }

        // 没有 SpEL，走默认的 UserHandler findDataScope
        return buildDefaultPermissionSql(alias, deptField, userField);
    }

    private String buildSpelPermissionSql(String alias, String column, String spel, Method method) {
        List<Long> values = parseSpelValues(spel, method);
        if (CollUtil.isEmpty(values)) {
            return ""; // SpEL 结果为空就不拼接
        }
        String prefix = buildAliasPrefix(alias);
        return prefix + column + " in (" + values.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
    }

    private String buildDefaultPermissionSql(String alias, String deptField, String userField) {
        String prefix = buildAliasPrefix(alias);

        long userId = StpUtil.getLoginIdAsLong();
        UserHandler userHandler = SpringUtil.getBean(UserHandler.class);
        List<Long> dataScope = userHandler.findDataScopeByUserId(userId);
        if (dataScope == null) {
            return "1 = 1";
        } else if (dataScope.size() == 1 && dataScope.contains(-1L)) {
            // 仅自己
            return prefix + userField + " = " + userId;
        } else if (dataScope.isEmpty()) {
            throw new CommonException(HttpStatus.UNAUTHORIZED.value(), "没有权限");
        }
        return prefix + deptField + " in (" + dataScope.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
    }

    private List parseSpelValues(String spel, Method method) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (beanResolver == null) {
            beanResolver = new BeanFactoryResolver(SpringUtil.getApplicationContext());
        }
        context.setBeanResolver(beanResolver);

        String[] paramNames = PARAM_DISCOVERER.getParameterNames(method);
        for (String name : paramNames) {
            context.setVariable(name, null); // 若需真实参数，应从 AOP 拦截器传入 ThreadLocal 提供
        }
        return Optional.ofNullable(SPEL_PARSER.parseExpression(spel).getValue(context, List.class)).orElse(Collections.emptyList());
    }

    private String buildAliasPrefix(String alias) {
        return StrUtil.isNotBlank(alias) ? alias + "." : "";
    }
}
