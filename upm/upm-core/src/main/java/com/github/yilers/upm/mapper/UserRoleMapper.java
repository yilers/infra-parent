package com.github.yilers.upm.mapper;

import com.github.yilers.upm.dto.UserRoleListDTO;
import com.github.yilers.upm.entity.Role;
import com.github.yilers.upm.entity.UserRole;
import com.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleMapper extends CustomMapper<UserRole> {

    List<Role> findRoleListByUserId(@Param("userId") Long userId);

    List<UserRoleListDTO> findRoleByUserIdList(List<Long> idList);
}
