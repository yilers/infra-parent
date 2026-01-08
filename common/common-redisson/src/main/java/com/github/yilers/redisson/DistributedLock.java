package com.github.yilers.redisson;

import java.lang.annotation.*;

/**
 * 分布式锁
 * @author zhanghui
 * @date 2025/4/22 下午2:08
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的key，支持SpEL表达式，例如：'order:' + #orderId
     */
    String key();

    /**
     * 锁最大持有时间，单位秒
     * 默认-1，表示不设置，启用看门狗自动续期
     */
    long leaseTime() default -1;

    /**
     * 获取锁最大等待时间，单位秒
     * 默认0，表示不等待，立即返回获取结果
     */
    long waitTime() default 0;

}


