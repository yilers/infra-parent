package com.github.yilers.upm.mapper;

import com.github.yilers.upm.entity.Permission;
import com.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PermissionMapper extends CustomMapper<Permission> {

    List<Permission> findPermissionsByUserId(@Param("userId") Long userId,
                                             @Param("device") String device);
}
