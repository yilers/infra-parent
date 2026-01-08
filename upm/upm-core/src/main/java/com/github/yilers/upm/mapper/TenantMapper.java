package com.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.github.yilers.upm.entity.Tenant;
import com.github.yilers.web.mybatis.CustomMapper;

@InterceptorIgnore(tenantLine = "true")
public interface TenantMapper extends CustomMapper<Tenant> {
}
