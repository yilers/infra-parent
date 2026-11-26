package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONUtil;
import io.github.yilers.upm.response.TenantCurrentResponse;
import io.github.yilers.upm.i18n.UpmMessage;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.upm.i18n.UpmManagementMessage;
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

    /** 只使用服务端可信租户上下文，普通登录用户无需租户管理权限即可读取展示信息。 */
    public TenantCurrentResponse currentInfo() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new CommonException(UpmManagementMessage.TENANT_ACCESS_FORBIDDEN);
        }
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw new CommonException(UpmManagementMessage.TENANT_NOT_FOUND);
        }
        if (!CommonConst.YES.equals(tenant.getUsable())) {
            throw new CommonException(UpmMessage.LOGIN_TENANT_DISABLED);
        }
        String title = tenant.getName();
        String logo = null;
        if (StrUtil.isNotBlank(tenant.getExpand())) {
            try {
                var expand = JSONUtil.parseObj(tenant.getExpand());
                String configuredTitle = expand.getStr("name");
                if (StrUtil.isNotBlank(configuredTitle)) {
                    title = configuredTitle;
                }
                logo = expand.getStr("logo");
            } catch (Exception ignored) {
                // 历史扩展格式异常时使用默认展示，不让Logo配置影响普通用户进入系统。
            }
        }
        return new TenantCurrentResponse(tenant.getId(), tenant.getName(), tenant.getCode(), title, logo);
    }

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
            throw new CommonException(UpmManagementMessage.TENANT_PLATFORM_DELETE);
        }
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw new CommonException(UpmManagementMessage.TENANT_NOT_FOUND);
        }
        if (!CommonConst.YES.equals(tenant.getOperable())) {
            throw new CommonException(UpmManagementMessage.TENANT_NOT_OPERABLE);
        }

        List<Long> userIds = findTenantUserIds(tenantId);
        if (!tenantService.removeById(tenantId)) {
            throw new CommonException(UpmManagementMessage.TENANT_DELETE_CONFLICT);
        }
        withTenant(tenantId, () -> userIds.forEach(authHandler::logout));
    }

    private void checkPlatformAdministrator() {
        if (!CommonConst.PLATFORM_TENANT_ID.equals(RequestContextHolder.getTenantId())
                || !StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)) {
            throw new CommonException(UpmManagementMessage.TENANT_DELETE_FORBIDDEN);
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
