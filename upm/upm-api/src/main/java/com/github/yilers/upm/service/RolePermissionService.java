package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.upm.entity.Permission;
import com.github.yilers.upm.entity.RolePermission;

import java.util.List;

public interface RolePermissionService extends IService<RolePermission> {


    List<Permission> findPermissionListByRoleId(Long roleId);

    List<Permission> findPermissionListByRoleIdList(List<Long> roleIdList);


    void deleteByRoleId(Long roleId);

    void deleteByRoleIdAndDevice(Long roleId, String device);
}
