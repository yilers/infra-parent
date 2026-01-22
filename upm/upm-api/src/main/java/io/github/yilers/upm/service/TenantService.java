package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.request.TenantRequest;

public interface TenantService extends IService<Tenant> {

    void addTenant(TenantRequest dto);

    Tenant findByCode(String code);

    void updateTenant(TenantRequest dto);
}
