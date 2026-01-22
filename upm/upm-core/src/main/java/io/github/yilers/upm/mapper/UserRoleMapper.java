package io.github.yilers.upm.mapper;

import io.github.yilers.upm.dto.UserRoleListDTO;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.UserRole;
import io.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleMapper extends CustomMapper<UserRole> {

    List<Role> findRoleListByUserId(@Param("userId") Long userId);

    List<UserRoleListDTO> findRoleByUserIdList(List<Long> idList);
}
