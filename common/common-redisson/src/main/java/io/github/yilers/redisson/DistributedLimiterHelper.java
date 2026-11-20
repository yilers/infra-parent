package io.github.yilers.redisson;

import lombok.experimental.UtilityClass;
import org.redisson.api.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class DistributedLimiterHelper {

    private static RedissonClient redissonClient;

    public static void init(RedissonClient client) {
        DistributedLimiterHelper.redissonClient = client;
    }

    /**
     * 按每秒次数初始化限流器，保留原有编程式调用。
     */
    public void initRateLimiter(String name, long count) {
        initRateLimiter(name, count, 1, TimeUnit.SECONDS);
    }

    /**
     * 初始化所有节点共享的限流器；已存在的同名配置不会被覆盖。
     * 同一个key应始终使用一致规则，避免不同接口或节点争用配置。
     */
    public void initRateLimiter(String name, long count, long interval, TimeUnit intervalUnit) {
        validateRateLimiter(count, interval, intervalUnit, 0, TimeUnit.SECONDS);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(name);
        rateLimiter.trySetRate(RateType.OVERALL, count, Duration.ofMillis(toMillis(interval, intervalUnit, false)));
    }

    public boolean tryAcquireRateLimiter(String name) {
        return tryAcquireRateLimiter(name, 0, TimeUnit.SECONDS);
    }

    /**
     * 每次获取一个许可；等待为0时立即判断，正数时最多阻塞指定时长。
     * Redis异常直接传播，不静默放行。
     */
    public boolean tryAcquireRateLimiter(String name, long waitTimeout, TimeUnit waitTimeoutUnit) {
        long waitMillis = toMillis(waitTimeout, waitTimeoutUnit, true);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(name);
        return waitMillis == 0 ? rateLimiter.tryAcquire() : rateLimiter.tryAcquire(waitMillis, TimeUnit.MILLISECONDS);
    }

    /** 校验注解或编程式调用的次数、周期及等待时间，不访问Redis。 */
    public void validateRateLimiter(long count, long interval, TimeUnit intervalUnit,
                                   long waitTimeout, TimeUnit waitTimeoutUnit) {
        if (count <= 0) {
            throw new IllegalArgumentException("限流请求次数count必须大于0");
        }
        toMillis(interval, intervalUnit, false);
        toMillis(waitTimeout, waitTimeoutUnit, true);
    }

    /** Redisson限流使用毫秒精度，禁止把正数时长静默截断为0。 */
    private long toMillis(long value, TimeUnit unit, boolean waiting) {
        String field = waiting ? "waitTimeout" : "interval";
        if (unit == null) {
            throw new IllegalArgumentException(field + "的时间单位不能为空");
        }
        if (value < 0 || (!waiting && value == 0)) {
            throw new IllegalArgumentException(field + (waiting ? "不能小于0" : "必须大于0"));
        }
        long millis = unit.toMillis(value);
        if (value > 0 && millis == 0) {
            throw new IllegalArgumentException(field + "换算后不能小于1毫秒");
        }
        return millis;
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
