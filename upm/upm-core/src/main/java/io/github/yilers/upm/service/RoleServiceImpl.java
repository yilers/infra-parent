package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
    private final RoleMapper roleMapper;

    @Override
    public Role findByRoleCode(String roleCode) {
        LambdaQueryWrapper<Role> query = Wrappers.lambdaQuery(Role.class);
        query.eq(Role::getRoleCode, roleCode);
        return roleMapper.selectOne(query);
    }
}
