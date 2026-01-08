package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.upm.entity.Role;
import com.github.yilers.upm.entity.UserRole;

import java.util.List;
import java.util.Map;

public interface UserRoleService extends IService<UserRole> {

    List<Role> findRoleListByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);

    void saveUserRoleRelation(Long userId, List<Long> roleIdList);

    void deleteByUserId(Long userId);

    Map<Long, List<Role>> findRoleByUserIdList(List<Long> userIdList);

    Map<Long, Integer> findUserCountByRoleIdList(List<Long> roleIdList);

    List<Long> findUserIdListByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);

}
