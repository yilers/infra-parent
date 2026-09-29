package io.github.yilers.upm.handler;

import io.github.yilers.upm.entity.TenantLoginPolicy;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.TenantLoginPolicyService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginRiskHandlerTest {

    private final TenantLoginPolicyService policyService = mock(TenantLoginPolicyService.class);
    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final LoginRiskHandler handler = new LoginRiskHandler(policyService, redissonClient);

    @Test
    void tenantWithoutPolicyIsNotLimited() {
        User user = user();
        when(policyService.findByTenantId(1L)).thenReturn(null);

        assertNull(handler.check(user));
        verifyNoInteractions(redissonClient);
    }

    @Test
    void lockedAccountReportsRemainingMinutes() {
        User user = user();
        TenantLoginPolicy policy = policy();
        RBucket<String> lock = mock(RBucket.class);
        when(policyService.findByTenantId(1L)).thenReturn(policy);
        when(redissonClient.<String>getBucket("loginRisk:lock:1:10")).thenReturn(lock);
        when(lock.isExists()).thenReturn(true);
        when(lock.remainTimeToLive()).thenReturn(Duration.ofMinutes(4).toMillis() + 1);

        CommonException exception = assertThrows(CommonException.class, () -> handler.check(user));

        assertEquals("登录失败次数过多，请5分钟后再试", exception.getMessage());
    }

    @Test
    void reachingFailureLimitLocksAccount() {
        User user = user();
        TenantLoginPolicy policy = policy();
        RAtomicLong failures = mock(RAtomicLong.class);
        RBucket<String> lock = mock(RBucket.class);
        when(redissonClient.getAtomicLong("loginRisk:failure:1:10")).thenReturn(failures);
        when(redissonClient.<String>getBucket("loginRisk:lock:1:10")).thenReturn(lock);
        when(failures.incrementAndGet()).thenReturn(3L);
        when(lock.remainTimeToLive()).thenReturn(Duration.ofMinutes(10).toMillis());

        CommonException exception = assertThrows(CommonException.class,
                () -> handler.recordFailure(user, policy));

        assertEquals("登录失败次数过多，请10分钟后再试", exception.getMessage());
        verify(failures).expire(Duration.ofMinutes(10));
        verify(lock).set("1", Duration.ofMinutes(10));
        verify(failures).delete();
    }

    private User user() {
        User user = new User();
        user.setId(10L);
        user.setTenantId(1L);
        return user;
    }

    private TenantLoginPolicy policy() {
        TenantLoginPolicy policy = new TenantLoginPolicy();
        policy.setTenantId(1L);
        policy.setMaxFailures(3);
        policy.setFailureWindowMinutes(10);
        policy.setLockDurationMinutes(10);
        return policy;
    }
}
