package io.github.yilers.redisson;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
public class DistributedLockAspect {

    @Around("@annotation(DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        LockDefinition lockDefinition = LockMetadataCache.getLockDefinition(method);
        String spelKey = lockDefinition.getKey();
        // 解析SpEL表达式，结合参数和方法
        String lockKey = SpelExpressionParserUtil.parseKey(spelKey, method, joinPoint.getArgs());

        long waitTime = lockDefinition.getWaitTime();
        long leaseTime = lockDefinition.getLeaseTime();

        boolean locked = DistributedLockHelper.tryLock(lockKey, waitTime, leaseTime);
        if (!locked) {
            throw new RuntimeException("获取锁失败，key: " + lockKey);
        }
        try {
            return joinPoint.proceed();
        } finally {
            DistributedLockHelper.unlock(lockKey);
        }
    }
}
