package io.github.yilers.redisson;

import lombok.experimental.UtilityClass;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class DistributedLockHelper {
    private static final long AUTO_RENEW = -1L;

    // RedissonClient对象
    private static RedissonClient redissonClient;

    // 初始化RedissonClient
    public static void init(RedissonClient client) {
        // 初始化RedissonClient
        DistributedLockHelper.redissonClient = client;
    }

    /**
     * 尝试加锁
     *
     * @param lockKey   锁key
     * @param waitTimeout 等待时间，单位秒
     * @param lockTime 持有时间，单位秒；保留旧接口非正数表示自动续期的语义
     * @return 是否成功获得锁
     * @throws InterruptedException
     */
    public boolean tryLock(String lockKey, long waitTimeout, long lockTime) throws InterruptedException {
        // 保留原编程接口的秒单位，以及非正持有时间表示自动续期的既有语义。
        return tryLock(lockKey, waitTimeout, TimeUnit.SECONDS,
                lockTime > 0 ? lockTime : AUTO_RENEW, TimeUnit.SECONDS);
    }

    /** 等待时间与持有时间分别指定单位；统一转换为Redis使用的毫秒精度。 */
    public boolean tryLock(String lockKey, long waitTimeout, TimeUnit waitTimeoutUnit,
                           long lockTime, TimeUnit lockTimeUnit) throws InterruptedException {
        long waitMillis = validateAndConvert(waitTimeout, waitTimeoutUnit, false);
        long lockMillis = validateAndConvert(lockTime, lockTimeUnit, true);
        RLock lock = redissonClient.getLock(lockKey);
        if (lockMillis > 0) {
            return lock.tryLock(waitMillis, lockMillis, TimeUnit.MILLISECONDS);
        } else {
            // 不传固定持有时间，使用Redisson看门狗续期。
            return lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 释放锁，只有持有锁的线程才会释放，防止误释放
     *
     * @param lockKey
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 编程式加锁模板，自动释放锁（异常安全）
     *
     * @param lockKey
     * @param waitTimeout 等待时间，单位秒
     * @param lockTime 持有时间，单位秒；非正数表示自动续期
     * @param runnable
     * @throws InterruptedException
     */
    public void lockAndRun(String lockKey, long waitTimeout, long lockTime, Runnable runnable) throws InterruptedException {
        lockAndRun(lockKey, waitTimeout, TimeUnit.SECONDS,
                lockTime > 0 ? lockTime : AUTO_RENEW, TimeUnit.SECONDS, runnable);
    }

    /** 使用独立时间单位的编程式锁，业务结束或抛出异常后释放锁。 */
    public void lockAndRun(String lockKey, long waitTimeout, TimeUnit waitTimeoutUnit,
                           long lockTime, TimeUnit lockTimeUnit, Runnable runnable) throws InterruptedException {
        boolean locked = tryLock(lockKey, waitTimeout, waitTimeoutUnit, lockTime, lockTimeUnit);
        if (!locked) {
            throw new RuntimeException("获取锁失败，key: " + lockKey);
        }
        try {
            runnable.run();
        } finally {
            unlock(lockKey);
        }
    }

    public CompletableFuture<RLock> tryLockAsync(String lockKey, long waitTimeout, long lockTime) {
        return tryLockAsync(lockKey, waitTimeout, TimeUnit.SECONDS,
                lockTime > 0 ? lockTime : AUTO_RENEW, TimeUnit.SECONDS);
    }

    /** 异步获取锁同样使用独立时间单位，失败时结果为null。 */
    public CompletableFuture<RLock> tryLockAsync(String lockKey, long waitTimeout, TimeUnit waitTimeoutUnit,
                                                long lockTime, TimeUnit lockTimeUnit) {
        long waitMillis = validateAndConvert(waitTimeout, waitTimeoutUnit, false);
        long lockMillis = validateAndConvert(lockTime, lockTimeUnit, true);
        RLock lock = redissonClient.getLock(lockKey);
        RFuture<Boolean> future = (lockMillis > 0)
                ? lock.tryLockAsync(waitMillis, lockMillis, TimeUnit.MILLISECONDS)
                : lock.tryLockAsync(waitMillis, TimeUnit.MILLISECONDS);
        return future.toCompletableFuture().thenApply(locked -> locked ? lock : null);
    }

    /** 防止非法配置或亚毫秒时间截断为0后，意外改变等待、固定到期的语义。 */
    private long validateAndConvert(long value, TimeUnit unit, boolean holdingLock) {
        if (unit == null) {
            throw new IllegalArgumentException("锁的时间单位不能为空");
        }
        if (holdingLock && value == AUTO_RENEW) {
            return AUTO_RENEW;
        }
        if (value < 0 || holdingLock && value == 0) {
            throw new IllegalArgumentException(holdingLock
                    ? "lockTime必须为正数或-1（自动续期）" : "waitTimeout不能为负数");
        }
        long millis = unit.toMillis(value);
        if (value > 0 && millis == 0) {
            throw new IllegalArgumentException("锁的时间配置不能小于1毫秒");
        }
        return millis;
    }

    public CompletableFuture<Void> unlockAsync(RLock lock) {
        if (lock == null) {
            return CompletableFuture.completedFuture(null);
        }
        return lock.unlockAsync().toCompletableFuture();
    }

    public static DistributedLockChain buildChain() {
        return new DistributedLockChain(redissonClient);
    }

}
