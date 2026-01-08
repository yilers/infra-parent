package com.github.yilers.redisson;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DistributedLockChain {

    private final RedissonClient redissonClient;
    private final List<String> lockKeys = new ArrayList<>();
    private final List<RLock> lockedLocks = new ArrayList<>();

    public DistributedLockChain(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public DistributedLockChain addLockKey(String lockKey) {
        lockKeys.add(lockKey);
        return this;
    }

    public boolean tryLockAll(long waitTime, long leaseTime) throws InterruptedException {
        for (String key : lockKeys) {
            RLock lock = redissonClient.getLock(key);
            boolean locked;
            if (leaseTime > 0) {
                locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            } else {
                locked = lock.tryLock(waitTime, TimeUnit.SECONDS);
            }
            if (!locked) {
                unlockAll();
                return false;
            }
            lockedLocks.add(lock);
        }
        return true;
    }

    public void unlockAll() {
        for (RLock lock : lockedLocks) {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        lockedLocks.clear();
    }
}