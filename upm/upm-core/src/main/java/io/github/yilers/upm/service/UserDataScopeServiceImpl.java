package io.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.core.enums.DataScopeEnum;
import io.github.yilers.upm.entity.UserDataScope;
import io.github.yilers.upm.mapper.UserDataScopeMapper;
import io.github.yilers.upm.request.UserDataScopeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDataScopeServiceImpl extends ServiceImpl<UserDataScopeMapper, UserDataScope> implements UserDataScopeService {
    private final UserDataScopeMapper userDataScopeMapper;

    @Override
    @CacheInvalidate(name = "userDataScope:", key = "#request.userId")
    public void deleteByUserIdAndInterface(Long userId, String interfacePath) {
        LambdaUpdateWrapper<UserDataScope> update = Wrappers.lambdaUpdate(UserDataScope.class);
        update.eq(UserDataScope::getUserId, userId);
        update.eq(UserDataScope::getInterfacePath, interfacePath);
        userDataScopeMapper.delete(update);
    }

    @Override
    @CacheInvalidate(name = "userDataScope:", key = "#request.userId")
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
        LambdaUpdateWrapper<UserDataScope> update = Wrappers.lambdaUpdate(UserDataScope.class);
        update.eq(UserDataScope::getUserId, userId);
        userDataScopeMapper.delete(update);
        List<String> interfacePathList = request.getInterfacePathList();
        List<UserDataScope> collect = interfacePathList.stream().map(item -> {
            UserDataScope userDataScope = new UserDataScope();
            userDataScope.setUserId(userId);
            userDataScope.setInterfacePath(item);
            userDataScope.setDataScope(DataScopeEnum.MY_SELF.getValue());
            return userDataScope;
        }).collect(Collectors.toList());
        saveBatch(collect);
    }

}
