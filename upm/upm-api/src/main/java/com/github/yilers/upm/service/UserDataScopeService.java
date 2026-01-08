package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.upm.entity.UserDataScope;
import com.github.yilers.upm.request.UserDataScopeRequest;

import java.util.List;

public interface UserDataScopeService extends IService<UserDataScope> {


    void deleteByUserIdAndInterface(Long userId, String interfacePath);


    void deleteByUserId(Long userId);

    UserDataScope findByUserIdAndInterface(Long userId, String interfacePath);


    List<UserDataScope> findByUserId(Long userId);


    void bind(UserDataScopeRequest request);
}
