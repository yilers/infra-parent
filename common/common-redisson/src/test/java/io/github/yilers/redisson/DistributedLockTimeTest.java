package io.github.yilers.redisson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证独立时间单位真正传递到Redisson，而非仅在注解中声明。 */
class DistributedLockTimeTest {
    private final RedissonClient client = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);

    @BeforeEach
    void initialize() {
        DistributedLockHelper.init(client);
        when(client.getLock("order")).thenReturn(lock);
    }

    @AfterEach
    void clear() {
        DistributedLockHelper.init(null);
    }

    @Test
    void waitingInMillisecondsAndHoldingInSecondsAreConvertedIndependently() throws Exception {
        when(lock.tryLock(200, 30000, TimeUnit.MILLISECONDS)).thenReturn(true);
        assertTrue(DistributedLockHelper.tryLock("order", 200, TimeUnit.MILLISECONDS, 30, TimeUnit.SECONDS));
        verify(lock).tryLock(200, 30000, TimeUnit.MILLISECONDS);
    }

    @Test
    void waitingInSecondsAndHoldingInMillisecondsAreConvertedIndependently() throws Exception {
        when(lock.tryLock(2000, 500, TimeUnit.MILLISECONDS)).thenReturn(true);
        assertTrue(DistributedLockHelper.tryLock("order", 2, TimeUnit.SECONDS, 500, TimeUnit.MILLISECONDS));
    }

    @Test
    void defaultWatchdogDoesNotUseFixedHoldingTime() throws Exception {
        when(lock.tryLock(200, TimeUnit.MILLISECONDS)).thenReturn(true);
        assertTrue(DistributedLockHelper.tryLock("order", 200, TimeUnit.MILLISECONDS, -1, TimeUnit.SECONDS));
        verify(lock).tryLock(200, TimeUnit.MILLISECONDS);
        verify(lock, never()).tryLock(anyLong(), anyLong(), any());
    }

    @Test
    void existingProgrammingOverloadRetainsSecondsAndWatchdogSemantics() throws Exception {
        when(lock.tryLock(2000, TimeUnit.MILLISECONDS)).thenReturn(true);
        assertTrue(DistributedLockHelper.tryLock("order", 2, 0));
        verify(lock).tryLock(2000, TimeUnit.MILLISECONDS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void asynchronousLockUsesSameTimeConversion() {
        RFuture<Boolean> future = mock(RFuture.class);
        when(future.toCompletableFuture()).thenReturn(CompletableFuture.completedFuture(true));
        when(lock.tryLockAsync(200, 30000, TimeUnit.MILLISECONDS)).thenReturn(future);
        assertSame(lock, DistributedLockHelper.tryLockAsync("order", 200, TimeUnit.MILLISECONDS,
                30, TimeUnit.SECONDS).join());
    }

    @Test
    void invalidTimesFailBeforeCallingRedis() {
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLockHelper.tryLock("order", -2, TimeUnit.SECONDS, 30, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLockHelper.tryLock("order", 0, TimeUnit.SECONDS, 0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLockHelper.tryLock("order", 0, null, -1, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class,
                () -> DistributedLockHelper.tryLock("order", 0, TimeUnit.SECONDS, 1, TimeUnit.MICROSECONDS));
        verifyNoInteractions(client);
    }
}
