package com.github.yilers.redisson;

import lombok.experimental.UtilityClass;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class DistributedLockHelper {

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
     * @param waitTime  等待时间，单位秒
     * @param leaseTime 持有时间，单位秒，-1表示不设置启用看门狗自动续期
     * @return 是否成功获得锁
     * @throws InterruptedException
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) throws InterruptedException {
        RLock lock = redissonClient.getLock(lockKey);
        if (leaseTime > 0) {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } else {
            // leaseTime <= 0，启用看门狗机制，默认30秒续期
            return lock.tryLock(waitTime, TimeUnit.SECONDS);
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
     * @param waitTime
     * @param leaseTime
     * @param runnable
     * @throws InterruptedException
     */
    public void lockAndRun(String lockKey, long waitTime, long leaseTime, Runnable runnable) throws InterruptedException {
        boolean locked = tryLock(lockKey, waitTime, leaseTime);
        if (!locked) {
            throw new RuntimeException("获取锁失败，key: " + lockKey);
        }
        try {
            runnable.run();
        } finally {
            unlock(lockKey);
        }
    }

    public CompletableFuture<RLock> tryLockAsync(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        RFuture<Boolean> future = (leaseTime > 0)
                ? lock.tryLockAsync(waitTime, leaseTime, TimeUnit.SECONDS)
                : lock.tryLockAsync(waitTime, TimeUnit.SECONDS);
        return future.toCompletableFuture().thenApply(locked -> locked ? lock : null);
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
