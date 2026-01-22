package io.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.mapper.UserMapper;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.response.UserInfoResponse;
import io.github.yilers.web.base.BasePageRequest;
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
            // 设置忽略租户插件
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            LambdaQueryWrapper<User> query = Wrappers.lambdaQuery(User.class);
            query.eq(User::getAccount, account);
            return userMapper.selectOne(query);
        } finally {
            // 关闭忽略策略
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
    @Cached(name = "user:currentInfo:", key = "#userId", cacheType = CacheType.REMOTE, expire = 600)
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
