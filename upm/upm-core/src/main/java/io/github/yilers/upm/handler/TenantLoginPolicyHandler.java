package io.github.yilers.upm.handler;

import cn.hutool.v7.core.bean.BeanUtil;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.entity.TenantLoginPolicy;
import io.github.yilers.upm.request.TenantLoginPolicyRequest;
import io.github.yilers.upm.service.TenantLoginPolicyService;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 租户账号密码登录策略业务处理。
 */
@Component
@RequiredArgsConstructor
public class TenantLoginPolicyHandler {

    public static final int DEFAULT_MAX_FAILURES = 3;
    public static final int DEFAULT_FAILURE_WINDOW_MINUTES = 10;
    public static final int DEFAULT_LOCK_DURATION_MINUTES = 10;

    private final TenantLoginPolicyService tenantLoginPolicyService;
    private final TenantService tenantService;

    public TenantLoginPolicy find(Long tenantId) {
        checkTenant(tenantId);
        return tenantLoginPolicyService.findByTenantId(tenantId);
    }

    /**
     * 新增或修改租户登录策略。
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(Long tenantId, TenantLoginPolicyRequest request) {
        checkTenant(tenantId);
        TenantLoginPolicy policy = tenantLoginPolicyService.findByTenantId(tenantId);
        if (policy == null) {
            policy = BeanUtil.copyProperties(request, TenantLoginPolicy.class);
            policy.setTenantId(tenantId);
            policy.setVersion(1);
            if (!tenantLoginPolicyService.save(policy)) {
                throw new CommonException("登录策略保存失败");
            }
            return;
        }
        if (request.getVersion() == null || !Objects.equals(policy.getVersion(), request.getVersion())) {
            throw new CommonException("登录策略已经变更，请刷新后重试");
        }
        policy.setMaxFailures(request.getMaxFailures());
        policy.setFailureWindowMinutes(request.getFailureWindowMinutes());
        policy.setLockDurationMinutes(request.getLockDurationMinutes());
        if (!tenantLoginPolicyService.updateById(policy)) {
            throw new CommonException("登录策略已经变更，请刷新后重试");
        }
    }

    /**
     * 物理删除策略，删除后该租户不再限制密码失败次数。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long tenantId) {
        checkTenant(tenantId);
        TenantLoginPolicy policy = tenantLoginPolicyService.findByTenantId(tenantId);
        if (policy == null) {
            return;
        }
        if (!tenantLoginPolicyService.removeById(policy.getId())) {
            throw new CommonException("登录策略删除失败，请刷新后重试");
        }
    }

    /**
     * 为新租户创建默认策略。
     */
    public void createDefault(Long tenantId) {
        TenantLoginPolicy policy = new TenantLoginPolicy();
        policy.setTenantId(tenantId);
        policy.setMaxFailures(DEFAULT_MAX_FAILURES);
        policy.setFailureWindowMinutes(DEFAULT_FAILURE_WINDOW_MINUTES);
        policy.setLockDurationMinutes(DEFAULT_LOCK_DURATION_MINUTES);
        policy.setVersion(1);
        if (!tenantLoginPolicyService.save(policy)) {
            throw new CommonException("租户默认登录策略创建失败");
        }
    }

    private void checkTenant(Long tenantId) {
        Tenant tenant = tenantId == null ? null : tenantService.getById(tenantId);
        if (tenant == null) {
            throw new CommonException("租户不存在");
        }
    }
}
