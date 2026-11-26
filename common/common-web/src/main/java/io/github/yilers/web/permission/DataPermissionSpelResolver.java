package io.github.yilers.web.permission;

import cn.hutool.v7.extra.spring.SpringUtil;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.i18n.CommonMessage;
import lombok.experimental.UtilityClass;
import org.apache.ibatis.annotations.Param;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 自定义行权限的统一表达式解析，只接受ID集合，不接受SQL片段。 */
@UtilityClass
public class DataPermissionSpelResolver {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();
    private static final ConcurrentMap<String, Expression> EXPRESSIONS = new ConcurrentHashMap<>();

    /**
     * 切面传入真实参数；Mapper注解回退没有参数时，仅能解析不依赖参数的表达式。
     * null、错误类型及解析异常均作为配置错误拒绝，空集合表示无可访问数据。
     */
    public List<Long> resolve(String spel, Method method, Object[] arguments) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            ApplicationContext applicationContext = SpringUtil.getApplicationContext();
            if (applicationContext != null) {
                context.setBeanResolver(new BeanFactoryResolver(applicationContext));
            }
            if (method != null && arguments != null) {
                if (method.getParameterCount() != arguments.length) {
                    throw new IllegalArgumentException("数据权限方法参数数量不一致");
                }
                for (int i = 0; i < arguments.length; i++) {
                    context.setVariable("arg" + i, arguments[i]);
                    context.setVariable("p" + i, arguments[i]);
                    context.setVariable("a" + i, arguments[i]);
                }
                // CGLIB调用拿到的是实现方法，接口上的@Param也应保留。
                for (Class<?> api : ClassUtils.getAllInterfacesForClassAsSet(method.getDeclaringClass())) {
                    try {
                        bindNamedParameters(context, api.getMethod(method.getName(), method.getParameterTypes()), arguments);
                    } catch (NoSuchMethodException ignored) {
                        // 该接口没有声明本方法。
                    }
                }
                bindNamedParameters(context, method, arguments);
            }
            Object value = EXPRESSIONS.computeIfAbsent(spel, PARSER::parseExpression).getValue(context);
            if (!(value instanceof Collection<?> values)) {
                throw new IllegalArgumentException("自定义数据权限表达式必须返回非null的ID集合");
            }
            // 转为Long后再参与SQL构造，拒绝null、浮点数及SQL片段，不拼接原始返回值。
            return values.stream().map(item -> Long.valueOf(String.valueOf(item))).distinct().toList();
        } catch (Exception e) {
            CommonException failure = new CommonException(CommonMessage.DATA_PERMISSION_INVALID);
            failure.initCause(e);
            throw failure;
        }
    }

    private void bindNamedParameters(StandardEvaluationContext context, Method method, Object[] arguments) {
        String[] names = PARAMETER_NAMES.getParameterNames(method);
        var parameters = method.getParameters();
        for (int i = 0; i < arguments.length; i++) {
            if (names != null) {
                context.setVariable(names[i], arguments[i]);
            }
            Param param = parameters[i].getAnnotation(Param.class);
            if (param != null) {
                context.setVariable(param.value(), arguments[i]);
            }
        }
    }
}
