package com.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.upm.entity.User;
import com.github.yilers.upm.request.UserPageRequest;
import com.github.yilers.upm.response.UserInfoResponse;
import com.github.yilers.web.base.BasePageRequest;

import java.util.List;

public interface UserService extends IService<User> {

    User findByAccount(String account);

    List<User> findByDeptId(Long deptId);

    UserInfoResponse currentInfo(Long userId);

    Page<UserInfoResponse> findByPage(Page<?> p, BasePageRequest<UserPageRequest> request);

    @InterceptorIgnore(tenantLine = "true")
    @Cached(name = "user:", key = "#userId", cacheType = CacheType.REMOTE, expire = 600)
    default User findById(Long userId) {
        return this.getById(userId);
    }

    boolean existByPositionId(Long positionId);

    @CacheInvalidate(name = "user:", key = "#userId")
    @CacheInvalidate(name = "user:currentInfo:", key = "#userId")
    default void cleanCache(Long userId) {
    }
}
