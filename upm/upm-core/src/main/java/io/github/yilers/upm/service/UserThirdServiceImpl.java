package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.UserThird;
import io.github.yilers.upm.mapper.UserThirdMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserThirdServiceImpl extends ServiceImpl<UserThirdMapper, UserThird> implements UserThirdService {
    private final UserThirdMapper userThirdMapper;

    @Override
    public UserThird findByOpenId(String openId) {
        LambdaQueryWrapper<UserThird> query = Wrappers.lambdaQuery(UserThird.class);
        query.eq(UserThird::getOpenId, openId);
        return userThirdMapper.selectOne(query);
    }

    @Override
    public UserThird findByUnionId(String unionId) {
        LambdaQueryWrapper<UserThird> query = Wrappers.lambdaQuery(UserThird.class);
        query.eq(UserThird::getUnionId, unionId);
        return userThirdMapper.selectOne(query);
    }

    @Override
    public UserThird findByUserId(Long userId) {
        LambdaQueryWrapper<UserThird> query = Wrappers.lambdaQuery(UserThird.class);
        query.eq(UserThird::getUserId, userId);
        return userThirdMapper.selectOne(query);
    }
}
