package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yilers.upm.entity.Permission;
import com.github.yilers.upm.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
    private final PermissionMapper permissionMapper;

    @Override
    public List<Permission> findPermissionsByUserId(Long userId, String device) {
        return permissionMapper.findPermissionsByUserId(userId, device);
    }

    @Override
    public Permission findByPermissionCode(String permissionCode) {
        LambdaQueryWrapper<Permission> query = Wrappers.lambdaQuery(Permission.class);
        query.eq(Permission::getPermissionCode, permissionCode);
        return permissionMapper.selectOne(query);
    }

    @Override
    public List<Permission> findAllByDevice(String device) {
        LambdaQueryWrapper<Permission> query = Wrappers.lambdaQuery(Permission.class);
        query.eq(Permission::getDevice, device);
        return permissionMapper.selectList(query);
    }

    @Override
    public List<Permission> findByParentId(Long parentId, Integer usable) {
        LambdaQueryWrapper<Permission> query = Wrappers.lambdaQuery(Permission.class);
        query.eq(Permission::getParentId, parentId);
        query.eq(usable != null, Permission::getUsable, usable);
        return permissionMapper.selectList(query);
    }
}
