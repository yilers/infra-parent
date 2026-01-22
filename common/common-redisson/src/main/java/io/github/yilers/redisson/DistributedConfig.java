package io.github.yilers.redisson;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

//@Configuration
public class DistributedConfig {

    @Autowired
    public void initRedisson(RedissonClient redissonClient) {
        DistributedLockHelper.init(redissonClient);
        DistributedLimiterHelper.init(redissonClient);
    }

    @Bean
    public DistributedLockAspect distributedLockAspect() {
        return new DistributedLockAspect();
    }

    @Bean
    public DistributedRateLimiterAspect distributedRateLimiterAspect() {
        return new DistributedRateLimiterAspect();
    }
}
