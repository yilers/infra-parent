package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.yilers.upm.entity.UserThird;

public interface UserThirdService extends IService<UserThird> {

    UserThird findByOpenId(String openId);

    UserThird findByUnionId(String unionId);

    UserThird findByUserId(Long userId);

}
