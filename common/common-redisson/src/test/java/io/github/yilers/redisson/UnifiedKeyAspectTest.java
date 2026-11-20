package io.github.yilers.redisson;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用真实Spring代理、模拟Redis工具调用，验证两类切面执行同一套key规则。 */
class UnifiedKeyAspectTest {
    @Test
    void interfaceProxyUsesImplementationParameterNamesForBothAnnotations() throws Exception {
        AspectJProxyFactory factory = new AspectJProxyFactory(new Operations());
        factory.setProxyTargetClass(false);
        factory.addAspect(new DistributedLockAspect());
        factory.addAspect(new DistributedRateLimiterAspect());
        OperationsApi proxy = factory.getProxy();
        try (var locks = mockStatic(DistributedLockHelper.class);
             var rates = mockStatic(DistributedLimiterHelper.class)) {
            locks.when(() -> DistributedLockHelper.tryLock("lock:20", 0, TimeUnit.SECONDS, -1, TimeUnit.SECONDS)).thenReturn(true);
            rates.when(() -> DistributedLimiterHelper.tryAcquireRateLimiter("rate:20", 0, TimeUnit.SECONDS)).thenReturn(true);
            assertEquals("done", proxy.process(new Request(20L)));
            locks.verify(() -> DistributedLockHelper.tryLock("lock:20", 0, TimeUnit.SECONDS, -1, TimeUnit.SECONDS));
            locks.verify(() -> DistributedLockHelper.unlock("lock:20"));
            rates.verify(() -> DistributedLimiterHelper.initRateLimiter("rate:20", 2, 1, TimeUnit.SECONDS));
        }
    }

    @Test
    void invalidKeyDoesNotAccessRedisOrExecuteBusinessMethod() {
        InvalidOperation target = new InvalidOperation();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new DistributedLockAspect());
        InvalidOperation proxy = factory.getProxy();
        try (var locks = mockStatic(DistributedLockHelper.class)) {
            assertThrows(IllegalArgumentException.class, proxy::process);
            locks.verifyNoInteractions();
            assertFalse(target.executed);
        }
    }

    @Test
    void independentUnitsReachLockHelperAndTimeoutDoesNotExecuteBusiness() throws Exception {
        TimedOperation target = new TimedOperation();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new DistributedLockAspect());
        TimedOperation proxy = factory.getProxy();
        try (var locks = mockStatic(DistributedLockHelper.class)) {
            // 模拟等待耗尽后未获取到锁，不需要实际等待或连接Redis。
            assertThrows(RuntimeException.class, proxy::process);
            locks.verify(() -> DistributedLockHelper.tryLock("timed", 200, TimeUnit.MILLISECONDS, 30, TimeUnit.SECONDS));
            locks.verify(() -> DistributedLockHelper.unlock("timed"), never());
            assertFalse(target.executed);
        }
    }

    public static class TimedOperation {
        boolean executed;

        @DistributedLock(key = "'timed'", waitTimeout = 200, waitTimeoutUnit = TimeUnit.MILLISECONDS,
                lockTime = 30, lockTimeUnit = TimeUnit.SECONDS)
        public void process() {
            executed = true;
        }
    }

    public interface OperationsApi {
        String process(Request payload);
    }

    public record Request(Long id) {
    }

    public static class Operations implements OperationsApi {
        @Override
        @DistributedLock(key = "'lock:' + #request.id")
        @DistributedRateLimiter(key = "'rate:' + #arg0.id", count = 2)
        public String process(Request request) {
            return "done";
        }
    }

    public static class InvalidOperation {
        boolean executed;

        @DistributedLock(key = "infra:unquoted")
        public void process() {
            executed = true;
        }
    }
}
