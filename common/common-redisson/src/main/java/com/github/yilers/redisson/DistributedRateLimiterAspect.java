package com.github.yilers.redisson;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
public class DistributedRateLimiterAspect {
    private static final Logger log = LoggerFactory.getLogger(DistributedRateLimiterAspect.class);
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ConcurrentHashMap<Method, Expression> spelCache = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedRateLimiter rateLimiter) throws Throwable {
        Method method = ((MethodSignature)joinPoint.getSignature()).getMethod();
        Expression expression = spelCache.computeIfAbsent(method, m -> parser.parseExpression(rateLimiter.key()));

        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        String key = expression.getValue(context, String.class);
        DistributedLimiterHelper.initRateLimiter(key, rateLimiter.permitsPerSecond());
        boolean allowed = DistributedLimiterHelper.tryAcquireRateLimiter(key);

        if (!allowed) {
            if (!rateLimiter.fallbackToPass()) {
                throw new RateLimitException(rateLimiter.message());
            } else {
                log.warn("Rate limit exceeded, falling back to pass");
            }
        }
        return joinPoint.proceed();
    }
}
