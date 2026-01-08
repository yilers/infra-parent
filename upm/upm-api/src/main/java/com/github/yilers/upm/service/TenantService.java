package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.upm.entity.Tenant;
import com.github.yilers.upm.request.TenantRequest;

public interface TenantService extends IService<Tenant> {

    void addTenant(TenantRequest dto);

    Tenant findByCode(String code);

    void updateTenant(TenantRequest dto);
}
