package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.github.yilers.upm.entity.TenantLoginPolicy;

/**
 * 租户账号密码登录策略服务。
 */
public interface TenantLoginPolicyService extends IService<TenantLoginPolicy> {

    /**
     * 根据租户查询登录策略。
     *
     * @param tenantId 租户ID
     * @return 登录策略；未配置时返回null
     */
    TenantLoginPolicy findByTenantId(Long tenantId);
}
