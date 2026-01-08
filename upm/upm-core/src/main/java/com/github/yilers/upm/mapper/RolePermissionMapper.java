package com.github.yilers.upm.mapper;

import com.github.yilers.upm.entity.Permission;
import com.github.yilers.upm.entity.RolePermission;
import com.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RolePermissionMapper extends CustomMapper<RolePermission> {

    List<Permission> findPermissionListByRoleIdList(@Param("roleIdList") List<Long> roleIdList);
}
