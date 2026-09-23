package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.github.yilers.upm.entity.ThirdAuthConfig;

/**
 * 第三方认证平台配置服务。
 */
public interface ThirdAuthConfigService extends IService<ThirdAuthConfig> {

    /**
     * 查询当前租户指定平台的配置。
     *
     * @param platform 平台编码
     * @return 平台配置，不存在时返回null
     */
    ThirdAuthConfig findByPlatform(String platform);
}
