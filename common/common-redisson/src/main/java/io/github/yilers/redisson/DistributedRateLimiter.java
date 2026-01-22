package io.github.yilers.redisson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式限流
 * @author zhanghui
 * @since 2025/4/22 下午2:08
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedRateLimiter {

    /**
     * 限流key，支持SpEL
     */
    String key();

    /**
     * 每秒最大允许请求数
     */
    long permitsPerSecond();

    /**
     * 是否在限流失败时依然放行
     */
    boolean fallbackToPass() default false;

    /**
     * 抛出异常时的提示信息
     */
    String message() default "请求频率过高，请稍后重试";

}