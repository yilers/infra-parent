package io.github.yilers.upm.permission;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import io.github.yilers.upm.handler.UserHandler;
import io.github.yilers.upm.i18n.UpmExtraMessage;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.i18n.CommonMessage;
import io.github.yilers.web.permission.DataPermission;
import io.github.yilers.web.permission.DataPermissionContext;
import io.github.yilers.web.permission.DataPermissionContextHolder;
import io.github.yilers.web.permission.DataPermissionSpelResolver;
import io.github.yilers.web.permission.DataScope;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
public class CustomDataPermissionHandler implements MultiDataPermissionHandler {

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
            if (StrUtil.isNotBlank(columnValue)) {
                // 自定义范围与默认部门权限互斥，统一按ID集合构造条件并保留当前SQL别名。
                String condition = buildCustomPermissionSql(alias, column, ctx.getColumnValues());
                expressions.add(CCJSqlParserUtil.parseCondExpression(condition));
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

        // 配置了SpEL就必须使用自定义范围，字段遗漏或解析失败不能回退成默认权限。
        if (StrUtil.isNotBlank(spel)) {
            return buildSpelPermissionSql(alias, column, spel, method);
        }

        // 没有 SpEL，走默认的 UserHandler findDataScope
        return buildDefaultPermissionSql(alias, deptField, userField);
    }

    private String buildSpelPermissionSql(String alias, String column, String spel, Method method) {
        // 没有切面上下文时只能使用Bean、静态调用等不依赖方法参数的表达式。
        List<Long> values = DataPermissionSpelResolver.resolve(spel, method, null);
        return buildCustomPermissionSql(alias, column, values);
    }

    private String buildCustomPermissionSql(String alias, String column, List<Long> values) {
        if (StrUtil.isBlank(column) || !column.matches("[A-Za-z_][A-Za-z0-9_]*") || values == null) {
            throw new CommonException(CommonMessage.DATA_PERMISSION_INVALID);
        }
        if (values.isEmpty()) {
            // 明确没有可管理数据，不省略条件，也不回退默认部门范围。
            return "1 = 0";
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
            throw new CommonException(HttpStatus.UNAUTHORIZED.value(), UpmExtraMessage.NO_PERMISSION);
        }
        return prefix + deptField + " in (" + dataScope.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
    }

    private String buildAliasPrefix(String alias) {
        return StrUtil.isNotBlank(alias) ? alias + "." : "";
    }
}
