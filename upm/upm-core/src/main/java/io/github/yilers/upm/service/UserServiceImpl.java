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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("id", "u.id"), Map.entry("account", "u.account"), Map.entry("name", "u.name"),
            Map.entry("nickname", "u.nickname"), Map.entry("create_time", "u.create_time"),
            Map.entry("update_time", "u.update_time"), Map.entry("usable", "u.usable"),
            Map.entry("dept_name", "d.dept_name"), Map.entry("position_name", "p.position_name"),
            Map.entry("phone", "u.phone"), Map.entry("email", "u.email"), Map.entry("gender", "u.gender"));
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
    public List<User> findByAccountList(List<String> accountList) {
        if (accountList == null || accountList.isEmpty()) {
            return List.of();
        }
        try {
            // 与单账号查询保持一致：账号在所有未删除租户中唯一。
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            return userMapper.findByAccountList(accountList);
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
        PageSortHelper.apply(p, request, SORT_COLUMNS);
        return userMapper.findByPage(p, request);
    }

    @Override
    public boolean existByPositionId(Long positionId) {
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery(User.class);
        query.eq(User::getPositionId, positionId);
        return userMapper.exists(query);
    }

}
