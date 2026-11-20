package io.github.yilers.redisson;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;

@Aspect
public class DistributedRateLimiterAspect {
    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedRateLimiter rateLimiter) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());
        String key = SpelExpressionParserUtil.parseKey(rateLimiter.key(), method, joinPoint.getArgs());
        // 先校验全部参数，避免无效等待时间导致已创建Redis限流配置。
        DistributedLimiterHelper.validateRateLimiter(rateLimiter.count(), rateLimiter.interval(),
                rateLimiter.intervalUnit(), rateLimiter.waitTimeout(), rateLimiter.waitTimeoutUnit());
        DistributedLimiterHelper.initRateLimiter(key, rateLimiter.count(), rateLimiter.interval(), rateLimiter.intervalUnit());
        boolean allowed = DistributedLimiterHelper.tryAcquireRateLimiter(key, rateLimiter.waitTimeout(), rateLimiter.waitTimeoutUnit());

        if (!allowed) {
            throw new RateLimitException(rateLimiter.message());
        }
        return joinPoint.proceed();
    }
}
