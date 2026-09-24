package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.github.yilers.upm.entity.UserThird;

public interface UserThirdService extends IService<UserThird> {

    UserThird findByOpenId(String openId);

    UserThird findByOpenIdAndPlatform(String openId, String platform);

    UserThird findByUnionId(String unionId);

    UserThird findByUnionIdAndPlatform(String unionId, String platform);

    UserThird findByUserIdAndPlatform(Long userId, String platform);

    /**
     * 物理删除指定用户的第三方账号绑定，允许账号后续重新绑定。
     */
    boolean physicallyDelete(Long tenantId, Long userId, String platform);

}
