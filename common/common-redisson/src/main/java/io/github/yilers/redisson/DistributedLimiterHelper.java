package io.github.yilers.redisson;

import lombok.experimental.UtilityClass;
import org.redisson.api.*;

import java.time.Duration;

@UtilityClass
public class DistributedLimiterHelper {

    private static RedissonClient redissonClient;

    public static void init(RedissonClient client) {
        DistributedLimiterHelper.redissonClient = client;
    }

    // 限流器，固定速率，permitsPerSecond 每秒允许多少个请求
    public void initRateLimiter(String name, long permitsPerSecond) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(name);
        rateLimiter.trySetRate(RateType.OVERALL, permitsPerSecond, Duration.ofSeconds(1L));
    }

    public boolean tryAcquireRateLimiter(String name) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(name);
        return rateLimiter.tryAcquire();
    }

    // 信号量，控制同时访问的最大数量
    public void initSemaphore(String name, int permits) {
        RSemaphore semaphore = redissonClient.getSemaphore(name);
        semaphore.trySetPermits(permits);
    }

    public boolean tryAcquireSemaphore(String name) throws InterruptedException {
        RSemaphore semaphore = redissonClient.getSemaphore(name);
        return semaphore.tryAcquire();
    }

    public void releaseSemaphore(String name) {
        RSemaphore semaphore = redissonClient.getSemaphore(name);
        semaphore.release();
    }
}
