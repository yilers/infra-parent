package io.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.mapper.UserMapper;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserMapper userMapper;

    @Override
    public User findByAccount(String account) {
        try {
            // 登录时尚未建立租户上下文，查询由SQL关联未删除租户完成隔离。
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            return userMapper.findByAccount(account);
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }

    }

    @Override
    public List<User> findByDeptId(Long deptId) {
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery(User.class);
        query.eq(User::getDeptId, deptId);
        return userMapper.selectList(query);
    }

    @Override
    @Cached(name = CommonConst.USER_CURRENT_INFO_CACHE_NAME, key = "#userId", cacheType = CacheType.REMOTE, expire = 600)
    public UserInfoResponse currentInfo(Long userId) {
        return userMapper.currentInfo(userId);
    }

    @Override
    public Page<UserInfoResponse> findByPage(Page<?> p, BasePageRequest<UserPageRequest> request) {
        return userMapper.findByPage(p, request);
    }

    @Override
    public boolean existByPositionId(Long positionId) {
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery(User.class);
        query.eq(User::getPositionId, positionId);
        return userMapper.exists(query);
    }

}
