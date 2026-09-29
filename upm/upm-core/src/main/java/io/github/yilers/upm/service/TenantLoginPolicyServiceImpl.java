package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.TenantLoginPolicy;
import io.github.yilers.upm.mapper.TenantLoginPolicyMapper;
import org.springframework.stereotype.Service;

/**
 * 租户账号密码登录策略服务实现。
 */
@Service
public class TenantLoginPolicyServiceImpl
        extends ServiceImpl<TenantLoginPolicyMapper, TenantLoginPolicy>
        implements TenantLoginPolicyService {

    @Override
    public TenantLoginPolicy findByTenantId(Long tenantId) {
        return getOne(Wrappers.<TenantLoginPolicy>lambdaQuery()
                .eq(TenantLoginPolicy::getTenantId, tenantId));
    }
}
