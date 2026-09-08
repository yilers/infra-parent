package io.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.core.enums.DataScopeEnum;
import io.github.yilers.upm.entity.UserDataScope;
import io.github.yilers.upm.mapper.DeptMapper;
import io.github.yilers.upm.mapper.UserDataScopeMapper;
import io.github.yilers.upm.mapper.UserMapper;
import io.github.yilers.upm.request.UserDataScopeItemRequest;
import io.github.yilers.upm.request.UserDataScopeRequest;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDataScopeServiceImpl extends ServiceImpl<UserDataScopeMapper, UserDataScope> implements UserDataScopeService {
    private final UserDataScopeMapper userDataScopeMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;

    @Override
    @CacheInvalidate(name = "userDataScope:", key = "#userId")
    public void deleteByUserIdAndInterface(Long userId, String interfacePath) {
        LambdaUpdateWrapper<UserDataScope> update = Wrappers.lambdaUpdate(UserDataScope.class);
        update.eq(UserDataScope::getUserId, userId);
        update.eq(UserDataScope::getInterfacePath, interfacePath);
        userDataScopeMapper.delete(update);
    }

    @Override
    @CacheInvalidate(name = "userDataScope:", key = "#userId")
    public void deleteByUserId(Long userId) {
        LambdaUpdateWrapper<UserDataScope> update = Wrappers.lambdaUpdate(UserDataScope.class);
        update.eq(UserDataScope::getUserId, userId);
        userDataScopeMapper.delete(update);
    }

    @Override
    public UserDataScope findByUserIdAndInterface(Long userId, String interfacePath) {
        LambdaQueryWrapper<UserDataScope> query = Wrappers.lambdaQuery(UserDataScope.class);
        query.eq(UserDataScope::getUserId, userId);
        query.eq(UserDataScope::getInterfacePath, interfacePath);
        return userDataScopeMapper.selectOne(query);
    }

    @Override
    @Cached(name = "userDataScope:", key = "#userId", cacheType = CacheType.REMOTE, expire = 3600)
    public List<UserDataScope> findByUserId(Long userId) {
        LambdaQueryWrapper<UserDataScope> query = Wrappers.lambdaQuery(UserDataScope.class);
        query.eq(UserDataScope::getUserId, userId);
        return userDataScopeMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = "userDataScope:", key = "#request.userId")
    public void bind(UserDataScopeRequest request) {
        Long userId = request.getUserId();
        if (userMapper.selectById(userId) == null) {
            throw new CommonException("用户不存在或不属于当前租户");
        }
        List<UserDataScope> collect = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        for (UserDataScopeItemRequest item : request.getScopes()) {
            String path = item.getInterfacePath();
            if (!path.startsWith("/") || !path.equals(path.trim()) || !paths.add(path)) {
                throw new CommonException("接口路径格式错误或重复");
            }
            UserDataScope scope = new UserDataScope();
            scope.setUserId(userId);
            scope.setInterfacePath(path);
            scope.setDataScope(item.getDataScope());
            scope.setExpand("");
            if (DataScopeEnum.CUSTOM.getValue().equals(item.getDataScope())) {
                List<Long> ids = item.getDeptIdList();
                if (ids == null || ids.isEmpty()) {
                    throw new CommonException("自定义数据权限必须选择部门");
                }
                List<Long> deptIds = ids.stream().distinct().toList();
                if (deptMapper.selectByIds(deptIds).size() != deptIds.size()) {
                    throw new CommonException("部门不存在或不属于当前租户");
                }
                String expand = deptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                if (expand.length() > 500) {
                    throw new CommonException("自定义部门数量超出存储范围");
                }
                scope.setExpand(expand);
            }
            collect.add(scope);
        }
        // 全量替换用户覆盖配置，未提交的接口恢复沿用角色。
        LambdaUpdateWrapper<UserDataScope> update = Wrappers.lambdaUpdate(UserDataScope.class);
        update.eq(UserDataScope::getUserId, userId);
        userDataScopeMapper.delete(update);
        if (!collect.isEmpty()) {
            saveBatch(collect);
        }
    }

}
