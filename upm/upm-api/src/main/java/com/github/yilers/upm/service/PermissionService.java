package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.upm.entity.Permission;

import java.util.List;

public interface PermissionService extends IService<Permission> {

    List<Permission> findPermissionsByUserId(Long userId, String device);

    Permission findByPermissionCode(String permissionCode);

    List<Permission> findAllByDevice(String device);

    List<Permission> findByParentId(Long parentId, Integer usable);
}
