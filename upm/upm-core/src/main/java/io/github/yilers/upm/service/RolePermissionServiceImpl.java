package io.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.RolePermission;
import io.github.yilers.upm.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@InterceptorIgnore(tenantLine = "true")
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission> implements RolePermissionService {
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    @Cached(name="rolePermission:", key="#roleId", cacheType = CacheType.REMOTE, expire = 3600)
    public List<Permission> findPermissionListByRoleId(Long roleId) {
        return findPermissionListByRoleIdList(Collections.singletonList(roleId));
    }

    @Override
    public List<Permission> findPermissionListByRoleIdList(List<Long> roleIdList) {
        return rolePermissionMapper.findPermissionListByRoleIdList(roleIdList);
    }

    @Override
    @CacheInvalidate(name="rolePermission:", key="#roleId")
    public void deleteByRoleId(Long roleId) {
        LambdaUpdateWrapper<RolePermission> update = Wrappers.lambdaUpdate(RolePermission.class);
        update.eq(RolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(update);
    }

    @Override
    @CacheInvalidate(name="rolePermission:", key="#roleId")
    public void deleteByRoleIdAndDevice(Long roleId, String device) {
        LambdaUpdateWrapper<RolePermission> update = Wrappers.lambdaUpdate(RolePermission.class);
        update.eq(RolePermission::getRoleId, roleId);
        update.eq(RolePermission::getDevice, device);
        rolePermissionMapper.delete(update);
    }
}
