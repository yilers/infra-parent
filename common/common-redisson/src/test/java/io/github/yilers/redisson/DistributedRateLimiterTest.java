package io.github.yilers.redisson;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 模拟Redis，验证周期、等待时间及超限后不执行业务的行为。 */
class DistributedRateLimiterTest {
    private RedissonClient client;
    private RRateLimiter limiter;

    @BeforeEach
    void setUp() {
        client = mock(RedissonClient.class);
        limiter = mock(RRateLimiter.class);
        when(client.getRateLimiter(anyString())).thenReturn(limiter);
        DistributedLimiterHelper.init(client);
    }

    @AfterEach
    void tearDown() {
        DistributedLimiterHelper.init(null);
    }

    @Test
    void defaultWindowIsOneSecondAndDoesNotWait() {
        DefaultOperation target = new DefaultOperation();
        DefaultOperation proxy = proxy(target);
        when(limiter.tryAcquire()).thenReturn(true);

        proxy.process();

        verify(limiter).trySetRate(RateType.OVERALL, 2, Duration.ofSeconds(1));
        verify(limiter).tryAcquire();
        assertTrue(target.executed);
    }

    @Test
    void windowAndWaitUseIndependentUnitsAndRejectExcessRequests() {
        TimedOperation target = new TimedOperation();
        TimedOperation proxy = proxy(target);

        RateLimitException exception = assertThrows(RateLimitException.class, proxy::process);

        assertEquals("导出过于频繁", exception.getMessage());
        verify(limiter).trySetRate(RateType.OVERALL, 5, Duration.ofMinutes(1));
        verify(limiter).tryAcquire(200, TimeUnit.MILLISECONDS);
        assertFalse(target.executed);
    }

    @Test
    void invalidAnnotationDoesNotAccessRedisOrExecuteBusiness() {
        InvalidOperation target = new InvalidOperation();
        InvalidOperation proxy = proxy(target);

        assertThrows(IllegalArgumentException.class, proxy::process);

        verifyNoInteractions(client);
        assertFalse(target.executed);
    }

    @Test
    void invalidArgumentsFailBeforeAccessingRedis() {
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLimiterHelper.initRateLimiter("test", 0));
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLimiterHelper.initRateLimiter("test", 1, 0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLimiterHelper.initRateLimiter("test", 1, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLimiterHelper.initRateLimiter("test", 1, 1, TimeUnit.MICROSECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLimiterHelper.tryAcquireRateLimiter("test", -1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLimiterHelper.tryAcquireRateLimiter("test", 1, TimeUnit.NANOSECONDS));
        verifyNoInteractions(client);
    }

    @Test
    void legacyHelperRetainsOneSecondWindowAndRedisErrorsPropagate() {
        DistributedLimiterHelper.initRateLimiter("test", 3);
        verify(limiter).trySetRate(RateType.OVERALL, 3, Duration.ofSeconds(1));
        IllegalStateException unavailable = new IllegalStateException("Redis不可用");
        when(limiter.tryAcquire()).thenThrow(unavailable);
        assertSame(unavailable, assertThrows(IllegalStateException.class,
                () -> proxy(new DefaultOperation()).process()));
    }

    private <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new DistributedRateLimiterAspect());
        return factory.getProxy();
    }

    public static class DefaultOperation {
        boolean executed;

        @DistributedRateLimiter(key = "'test'", count = 2)
        public void process() {
            executed = true;
        }
    }

    public static class TimedOperation {
        boolean executed;

        @DistributedRateLimiter(key = "'test'", count = 5, intervalUnit = TimeUnit.MINUTES,
                waitTimeout = 200, waitTimeoutUnit = TimeUnit.MILLISECONDS, message = "导出过于频繁")
        public void process() {
            executed = true;
        }
    }

    public static class InvalidOperation {
        boolean executed;

        @DistributedRateLimiter(key = "'test'", count = 2, waitTimeout = -1)
        public void process() {
            executed = true;
        }
    }
}
