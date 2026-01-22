package io.github.yilers.upm.mapper;

import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.RolePermission;
import io.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RolePermissionMapper extends CustomMapper<RolePermission> {

    List<Permission> findPermissionListByRoleIdList(@Param("roleIdList") List<Long> roleIdList);
}
