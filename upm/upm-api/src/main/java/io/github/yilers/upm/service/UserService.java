package io.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.response.UserInfoResponse;

import java.util.List;

public interface UserService extends IService<User> {

    User findByAccount(String account);

    List<User> findByDeptId(Long deptId);

    UserInfoResponse currentInfo(Long userId);

    Page<UserInfoResponse> findByPage(Page<?> p, BasePageRequest<UserPageRequest> request);

    @InterceptorIgnore(tenantLine = "true")
    @Cached(name = CommonConst.USER_CACHE_NAME, key = "#userId", cacheType = CacheType.REMOTE, expire = 600)
    default User findById(Long userId) {
        return this.getById(userId);
    }

    boolean existByPositionId(Long positionId);

    @CacheInvalidate(name = CommonConst.USER_CACHE_NAME, key = "#userId")
    @CacheInvalidate(name = CommonConst.USER_CURRENT_INFO_CACHE_NAME, key = "#userId")
    default void cleanCache(Long userId) {
    }
}
