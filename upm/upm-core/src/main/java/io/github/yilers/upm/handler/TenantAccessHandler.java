package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.i18n.UpmManagementMessage;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 租户表本身不参与租户插件过滤，管理入口必须显式校验目标租户。 */
@Component
public class TenantAccessHandler {
    public boolean isPlatformAdministrator() {
        return CommonConst.PLATFORM_TENANT_ID.equals(RequestContextHolder.getTenantId())
                && StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE);
    }

    public Long currentTenantId() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new CommonException(UpmManagementMessage.TENANT_ACCESS_FORBIDDEN);
        }
        return tenantId;
    }

    /** 普通管理用户只能访问本租户；跨租户需要平台租户下的平台管理员身份。 */
    public void checkTenant(Long tenantId) {
        Long currentTenantId = currentTenantId();
        if (tenantId == null || !Objects.equals(currentTenantId, tenantId) && !isPlatformAdministrator()) {
            throw new CommonException(UpmManagementMessage.TENANT_ACCESS_FORBIDDEN);
        }
    }

    public void checkPlatformAdministrator() {
        if (!isPlatformAdministrator()) {
            throw new CommonException(UpmManagementMessage.TENANT_ACCESS_FORBIDDEN);
        }
    }
}
