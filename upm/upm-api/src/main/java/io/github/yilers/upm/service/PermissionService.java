package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.github.yilers.upm.entity.Permission;

import java.util.List;

public interface PermissionService extends IService<Permission> {

    List<Permission> findPermissionsByUserId(Long userId, String device, Long appId);

    Permission findByPermissionCode(String permissionCode, Long appId);

    List<Permission> findAllByDevice(String device, Long appId);

    List<Permission> findByParentId(Long parentId, String device, Long appId);
}
