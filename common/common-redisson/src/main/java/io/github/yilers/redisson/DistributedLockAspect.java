package io.github.yilers.redisson;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;

@Aspect
public class DistributedLockAspect {

    @Around("@annotation(DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());

        LockDefinition lockDefinition = LockMetadataCache.getLockDefinition(method);
        if (lockDefinition == null) {
            lockDefinition = LockMetadataCache.getLockDefinition(signature.getMethod());
        }
        if (lockDefinition == null) {
            throw new IllegalStateException("未找到分布式锁注解，方法：" + method.toGenericString());
        }
        String spelKey = lockDefinition.getKey();
        // 解析SpEL表达式，结合参数和方法
        String lockKey = SpelExpressionParserUtil.parseKey(spelKey, method, joinPoint.getArgs());

        boolean locked = DistributedLockHelper.tryLock(lockKey,
                lockDefinition.getWaitTimeout(), lockDefinition.getWaitTimeoutUnit(),
                lockDefinition.getLockTime(), lockDefinition.getLockTimeUnit());
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
