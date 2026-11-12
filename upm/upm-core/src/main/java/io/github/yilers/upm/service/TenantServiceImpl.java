package io.github.yilers.upm.service;

import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.mapper.TenantMapper;
import io.github.yilers.upm.handler.TenantAccessHandler;
import io.github.yilers.upm.request.TenantRequest;
import io.github.yilers.upm.i18n.UpmManagementMessage;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {
    private final TenantMapper tenantMapper;
    private final TenantAccessHandler tenantAccessHandler;

    @Override
    public Tenant findByCode(String code) {
        LambdaQueryWrapper<Tenant> query = Wrappers.lambdaQuery(Tenant.class);
        query.eq(Tenant::getCode, code);
        return tenantMapper.selectOne(query);
    }

    @Override
    public void updateTenant(TenantRequest dto) {
        tenantAccessHandler.checkTenant(dto.getId());
        Tenant existing = tenantMapper.selectById(dto.getId());
        if (existing == null) {
            throw new CommonException(UpmManagementMessage.TENANT_NOT_FOUND);
        }
        Tenant tenant = findByCode(dto.getCode());
        if (tenant != null && !tenant.getId().equals(dto.getId())) {
            throw new CommonException(UpmManagementMessage.TENANT_CODE_EXISTS);
        }
        Tenant copy = CglibUtil.copy(dto, Tenant.class);
        // 更新业务字段不能把系统内置租户的不可操作标志改成可操作。
        copy.setOperable(existing.getOperable());
        int i = tenantMapper.updateById(copy);
        if (i == 0) {
            throw new CommonException(UpmManagementMessage.TENANT_UPDATE_FAILED);
        }
    }

    @Override
    public List<Tenant> findAll() {
        return tenantMapper.findAll();
    }
}
