package io.github.yilers.redisson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式限流
 * @author zhanghui
 * @since 2025/4/22 下午2:08
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedRateLimiter {

    /**
     * 限流key，与分布式锁共用SpEL解析规则。
     * 固定key写作 "'rate:export'"，动态key写作 "'rate:export:' + #userId"。
     * 支持T(...)静态调用、参数名及#arg0/#p0/#a0位置别名；结果不能为null或空白。
     */
    String key();

    /**
     * 限流周期内最大允许请求次数，必须大于0；每次方法调用消耗一次额度。
     */
    long count();

    /**
     * 限流周期长度，必须大于0，换算后不得小于1毫秒。
     */
    long interval() default 1;

    /**
     * 限流周期单位，默认秒。
     */
    TimeUnit intervalUnit() default TimeUnit.SECONDS;

    /**
     * 获取额度的最长等待时间，必须非负；0表示立即判断，超限直接拒绝。
     * 正数换算后不得小于1毫秒，等待期间会占用当前调用线程。
     */
    long waitTimeout() default 0;

    /**
     * 等待时间单位，默认秒，与限流周期单位独立。
     */
    TimeUnit waitTimeoutUnit() default TimeUnit.SECONDS;

    /**
     * 抛出异常时的提示信息
     */
    String message() default "请求频率过高，请稍后重试";

}
