package io.github.yilers.redisson;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 * @author zhanghui
 * @since 2025/4/22 下午2:08
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的key，始终按SpEL表达式解析。
     * 固定key写作 "'order:submit'"，动态key写作 "'order:' + #orderId"。
     * 支持T(...)静态调用、参数名及#arg0/#p0/#a0位置别名；结果不能为null或空白。
     */
    String key();

    /**
     * 固定持有时间，单位由lockTimeUnit指定。
     * 默认-1，启用看门狗续期并在方法结束后释放；正数表示到期释放，即使业务尚未结束。
     */
    long lockTime() default -1;

    /** 固定持有时间单位，默认秒；自动续期模式不使用此单位。 */
    TimeUnit lockTimeUnit() default TimeUnit.SECONDS;

    /**
     * 获取锁的最长等待时间，单位由waitTimeoutUnit指定。
     * 默认0，拿不到立即失败；超时后不执行业务方法。
     */
    long waitTimeout() default 0;

    /** 获取锁的等待时间单位，默认秒，可独立设置为毫秒。 */
    TimeUnit waitTimeoutUnit() default TimeUnit.SECONDS;

}


