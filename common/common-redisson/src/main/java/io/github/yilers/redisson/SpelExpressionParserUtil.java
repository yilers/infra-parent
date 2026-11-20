package io.github.yilers.redisson;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 分布式锁与限流共用的key解析器，所有key均按完整SpEL表达式计算。 */
public final class SpelExpressionParserUtil {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();

    // 同一方法可以同时配置锁和限流，缓存必须同时区分方法及表达式。
    private static final ConcurrentMap<ExpressionKey, Expression> EXPRESSIONS = new ConcurrentHashMap<>();

    private SpelExpressionParserUtil() {
    }

    private record ExpressionKey(Method method, String expression) {
    }

    /**
     * 解析SpEL表达式生成key
     *
     * @param spelExpression 完整SpEL表达式；固定key也需要单引号，如 "'order:'"
     * @param method         方法对象
     * @param args           方法参数值数组
     * @return 解析后的字符串key
     */
    public static String parseKey(String spelExpression, Method method, Object[] args) {
        if (method == null || args == null || method.getParameterCount() != args.length) {
            throw new IllegalArgumentException("key解析所需的方法和参数不匹配");
        }
        if (spelExpression == null || spelExpression.isBlank()) {
            throw new IllegalArgumentException("key表达式不能为空，方法：" + method.toGenericString());
        }
        try {
            Expression expression = EXPRESSIONS.computeIfAbsent(new ExpressionKey(method, spelExpression),
                    key -> PARSER.parseExpression(key.expression()));
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] names = PARAMETER_NAMES.getParameterNames(method);
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    context.setVariable(names[i], args[i]);
                }
            }
            // 位置别名不依赖编译参数名；两种注解统一支持这些别名。
            for (int i = 0; i < args.length; i++) {
                context.setVariable("arg" + i, args[i]);
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }
            String key = expression.getValue(context, String.class);
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key表达式结果不能为空，方法：" + method.toGenericString());
            }
            return key;
        } catch (ExpressionException exception) {
            // 不降级成固定字符串，避免配置错误使不同业务意外共用同一个key。
            throw new IllegalArgumentException("key的SpEL表达式解析失败，请检查表达式及参数，方法："
                    + method.toGenericString(), exception);
        }
    }
}
