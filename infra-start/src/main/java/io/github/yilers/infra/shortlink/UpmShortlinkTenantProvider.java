package io.github.yilers.infra.shortlink;

import io.github.yilers.shortlink.provider.ShortlinkTenantProvider;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 单体部署适配：短链模块不直接依赖UPM。 */
@Component
@RequiredArgsConstructor
public class UpmShortlinkTenantProvider implements ShortlinkTenantProvider {
    private final TenantMapper tenants;

    @Override
    public boolean isUsable(Long tenantId) {
        Tenant tenant = tenants.selectById(tenantId);
        return tenant != null && CommonConst.YES.equals(tenant.getUsable());
    }
}
