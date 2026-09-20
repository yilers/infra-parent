package io.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.baomidou.mybatisplus.spring.service.IService;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.UserRole;

import java.util.List;
import java.util.Map;

public interface UserRoleService extends IService<UserRole> {

    List<Role> findRoleListByUserId(Long userId);

    List<UserRole> findByRoleId(Long roleId);

    void saveUserRoleRelation(Long userId, List<Long> roleIdList);

    void deleteByUserId(Long userId);

    @CacheInvalidate(name = CommonConst.USER_ROLE_CACHE_NAME, key = "#userId")
    default void cleanCache(Long userId) {
    }

    Map<Long, List<Role>> findRoleByUserIdList(List<Long> userIdList);

    Map<Long, Integer> findUserCountByRoleIdList(List<Long> roleIdList);

    List<Long> findUserIdListByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);

}
