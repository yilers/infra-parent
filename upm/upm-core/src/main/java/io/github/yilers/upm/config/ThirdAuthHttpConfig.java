package io.github.yilers.upm.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 第三方认证HTTP客户端配置。
 *
 * <p>UPM可能被独立启动，也可能作为模块集成到业务系统中，因此不能依赖宿主应用一定提供
 * {@link RestClient.Builder}。业务系统如需配置代理、超时或连接池，可以声明同名Bean覆盖默认实现。</p>
 */
@Configuration(proxyBeanMethods = false)
public class ThirdAuthHttpConfig {

    public static final String THIRD_AUTH_REST_CLIENT = "thirdAuthRestClient";

    @Bean(THIRD_AUTH_REST_CLIENT)
    @ConditionalOnMissingBean(name = THIRD_AUTH_REST_CLIENT)
    public RestClient thirdAuthRestClient() {
        return RestClient.create();
    }
}
