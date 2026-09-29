package io.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.github.yilers.upm.entity.TenantLoginPolicy;
import io.github.yilers.web.mybatis.CustomMapper;

/**
 * 租户账号密码登录策略Mapper。
 *
 * <p>登录前没有租户上下文，因此关闭自动租户条件，所有查询必须显式携带tenant_id。</p>
 */
@InterceptorIgnore(tenantLine = "true")
public interface TenantLoginPolicyMapper extends CustomMapper<TenantLoginPolicy> {
}
