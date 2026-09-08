package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.mapper.PermissionMapper;
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
    public List<Permission> findPermissionsByUserId(Long userId, String device, Long appId) {
        return permissionMapper.findPermissionsByUserId(userId, device, appId);
    }

    @Override
    public Permission findByPermissionCode(String permissionCode, Long appId) {
        LambdaQueryWrapper<Permission> query = Wrappers.lambdaQuery(Permission.class);
        query.eq(Permission::getPermissionCode, permissionCode);
        query.eq(Permission::getAppId, appId);
        return permissionMapper.selectOne(query);
    }

    @Override
    public List<Permission> findAllByDevice(String device, Long appId) {
        LambdaQueryWrapper<Permission> query = Wrappers.lambdaQuery(Permission.class);
        query.eq(Permission::getDevice, device);
        query.eq(Permission::getAppId, appId);
        return permissionMapper.selectList(query);
    }

    @Override
    public List<Permission> findByParentId(Long parentId, String device, Long appId) {
        LambdaQueryWrapper<Permission> query = Wrappers.lambdaQuery(Permission.class);
        query.eq(Permission::getParentId, parentId);
        query.eq(Permission::getDevice, device);
        query.eq(Permission::getAppId, appId);
        return permissionMapper.selectList(query);
    }
}
