package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户业务处理。
 */
@Component
@RequiredArgsConstructor
public class TenantHandler {

    private final TenantService tenantService;
    private final UserService userService;
    private final AuthHandler authHandler;

    /**
     * 逻辑删除业务租户，并注销该租户下仍然有效的登录会话。
     *
     * <p>租户业务数据继续保留，便于审计和后续恢复；删除租户后不会再允许新会话登录。</p>
     *
     * @param tenantId 租户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long tenantId) {
        checkPlatformAdministrator();
        if (tenantId == null || CommonConst.PLATFORM_TENANT_ID.equals(tenantId)) {
            throw new CommonException("平台租户不能删除");
        }
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw new CommonException("租户不存在");
        }
        if (!CommonConst.YES.equals(tenant.getOperable())) {
            throw new CommonException("该租户不可操作");
        }

        List<Long> userIds = findTenantUserIds(tenantId);
        if (!tenantService.removeById(tenantId)) {
            throw new CommonException("租户删除失败，数据已经变更");
        }
        withTenant(tenantId, () -> userIds.forEach(authHandler::logout));
    }

    private void checkPlatformAdministrator() {
        if (!CommonConst.PLATFORM_TENANT_ID.equals(RequestContextHolder.getTenantId())
                || !StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)) {
            throw new CommonException("只有平台管理员可以删除租户");
        }
    }

    private List<Long> findTenantUserIds(Long tenantId) {
        Long previousTenantId = RequestContextHolder.getTenantId();
        try {
            RequestContextHolder.setTenantId(tenantId);
            return userService.list().stream().map(User::getId).toList();
        } finally {
            RequestContextHolder.setTenantId(previousTenantId);
        }
    }

    private void withTenant(Long tenantId, Runnable action) {
        Long previousTenantId = RequestContextHolder.getTenantId();
        try {
            RequestContextHolder.setTenantId(tenantId);
            action.run();
        } finally {
            RequestContextHolder.setTenantId(previousTenantId);
        }
    }
}
