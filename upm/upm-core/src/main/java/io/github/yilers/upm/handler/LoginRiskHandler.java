package io.github.yilers.upm.handler;

import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.TenantLoginPolicy;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.TenantLoginPolicyService;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 账号密码登录风险控制。
 *
 * <p>租户没有登录策略时不做限制；失败次数和临时锁定状态仅保存在Redis中，
 * 不改变用户自身的启停状态。第三方认证和SSO不会经过该处理器。</p>
 */
@Component
@RequiredArgsConstructor
public class LoginRiskHandler {

    private final TenantLoginPolicyService tenantLoginPolicyService;
    private final RedissonClient redissonClient;

    /**
     * 查询租户策略并检查账号是否处于临时锁定状态。
     *
     * @param user 登录用户
     * @return 当前租户策略；未配置时返回null
     */
    public TenantLoginPolicy check(User user) {
        TenantLoginPolicy policy = tenantLoginPolicyService.findByTenantId(user.getTenantId());
        if (policy == null) {
            return null;
        }
        RBucket<String> lock = redissonClient.getBucket(lockKey(user));
        if (lock.isExists()) {
            throw lockedException(lock.remainTimeToLive(), policy.getLockDurationMinutes());
        }
        return policy;
    }

    /**
     * 记录一次密码错误；达到策略上限时立即锁定账号。
     */
    public void recordFailure(User user, TenantLoginPolicy policy) {
        if (policy == null) {
            return;
        }
        RAtomicLong failures = redissonClient.getAtomicLong(failureKey(user));
        long failureCount = failures.incrementAndGet();
        failures.expire(Duration.ofMinutes(policy.getFailureWindowMinutes()));
        if (failureCount < policy.getMaxFailures()) {
            return;
        }

        RBucket<String> lock = redissonClient.getBucket(lockKey(user));
        lock.set("1", Duration.ofMinutes(policy.getLockDurationMinutes()));
        failures.delete();
        throw lockedException(lock.remainTimeToLive(), policy.getLockDurationMinutes());
    }

    /**
     * 密码校验成功后清理历史失败状态。
     */
    public void clear(User user) {
        redissonClient.getAtomicLong(failureKey(user)).delete();
        redissonClient.getBucket(lockKey(user)).delete();
    }

    private CommonException lockedException(long remainMillis, int configuredMinutes) {
        long remainMinutes = remainMillis > 0
                ? Math.max(1, (remainMillis + Duration.ofMinutes(1).toMillis() - 1)
                / Duration.ofMinutes(1).toMillis())
                : configuredMinutes;
        return new CommonException("登录失败次数过多，请" + remainMinutes + "分钟后再试");
    }

    private String failureKey(User user) {
        return CommonConst.LOGIN_FAILURE_CACHE_NAME + user.getTenantId() + ":" + user.getId();
    }

    private String lockKey(User user) {
        return CommonConst.LOGIN_LOCK_CACHE_NAME + user.getTenantId() + ":" + user.getId();
    }
}
