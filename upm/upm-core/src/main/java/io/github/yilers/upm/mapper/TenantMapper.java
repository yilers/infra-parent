package io.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.web.mybatis.CustomMapper;

@InterceptorIgnore(tenantLine = "true")
public interface TenantMapper extends CustomMapper<Tenant> {
}
