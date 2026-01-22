package io.github.yilers.redisson;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SpelExpressionParserUtil {

    private static final ExpressionParser parser = new SpelExpressionParser();

    private static final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

    // 缓存方法对应的SpEL表达式，key: method + spel表达式字符串，value: Expression
    private static final ConcurrentMap<String, Expression> expressionCache = new ConcurrentHashMap<>();

    /**
     * 解析SpEL表达式生成key
     *
     * @param spelExpression SpEL表达式，如 "'order:' + #orderId"
     * @param method         方法对象
     * @param args           方法参数值数组
     * @return 解析后的字符串key
     */
    public static String parseKey(String spelExpression, Method method, Object[] args) {
        if (!spelExpression.contains("#")) {
            // 如果没有参数表达式，直接返回常量字符串
            return spelExpression;
        }

        // 生成缓存key，防止同一方法多种表达式冲突
        String cacheKey = method.toGenericString() + ":" + spelExpression;

        Expression expression = expressionCache.computeIfAbsent(cacheKey, k -> parser.parseExpression(spelExpression));

        // 获取参数名称列表
        String[] paramNames = paramNameDiscoverer.getParameterNames(method);

        EvaluationContext context = new StandardEvaluationContext();

        // 把参数放入SpEL上下文，支持用参数名访问
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 也放入arg0,arg1...形式，兼容旧版本写法
        for (int i = 0; i < args.length; i++) {
            context.setVariable("arg" + i, args[i]);
        }

        return expression.getValue(context, String.class);
    }
}