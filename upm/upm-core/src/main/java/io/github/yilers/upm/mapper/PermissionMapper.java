package io.github.yilers.upm.mapper;

import io.github.yilers.upm.entity.Permission;
import io.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PermissionMapper extends CustomMapper<Permission> {

    List<Permission> findPermissionsByUserId(@Param("userId") Long userId,
                                             @Param("device") String device);
}
