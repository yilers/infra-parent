package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.ThirdAuthConfig;
import io.github.yilers.upm.mapper.ThirdAuthConfigMapper;
import org.springframework.stereotype.Service;

/**
 * 第三方认证平台配置服务实现。
 */
@Service
public class ThirdAuthConfigServiceImpl extends ServiceImpl<ThirdAuthConfigMapper, ThirdAuthConfig>
        implements ThirdAuthConfigService {

    @Override
    public ThirdAuthConfig findByPlatform(String platform) {
        return getOne(Wrappers.<ThirdAuthConfig>lambdaQuery()
                .eq(ThirdAuthConfig::getPlatform, platform));
    }
}
