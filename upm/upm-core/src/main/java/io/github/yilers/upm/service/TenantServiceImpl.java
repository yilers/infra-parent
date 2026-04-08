package io.github.yilers.upm.service;

import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.data.id.IdUtil;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.crypto.digest.BCrypt;
import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.core.enums.UserTypeEnum;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.mapper.TenantMapper;
import io.github.yilers.upm.request.TenantRequest;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {
    private final TenantMapper tenantMapper;

    @Override
    public Tenant findByCode(String code) {
        LambdaQueryWrapper<Tenant> query = Wrappers.lambdaQuery(Tenant.class);
        query.eq(Tenant::getCode, code);
        return tenantMapper.selectOne(query);
    }

    @Override
    public void updateTenant(TenantRequest dto) {
        Tenant tenant = findByCode(dto.getCode());
        if (tenant != null && !tenant.getId().equals(dto.getId())) {
            throw new CommonException("租户编码已存在");
        }
        Tenant copy = CglibUtil.copy(dto, Tenant.class);
        copy.setOperable(CommonConst.YES);
        tenantMapper.updateById(copy);
    }
}
