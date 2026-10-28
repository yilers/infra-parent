package io.github.yilers.auth.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 机器认证早于用户租户上下文恢复，禁止在未验签时读取客户端租户头。 */
@Configuration
public class ApiAuthenticationConfig implements WebMvcConfigurer {
    private final ApiSignatureInterceptor interceptor;

    public ApiAuthenticationConfig(ApiSignatureInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/open-api/**").order(-200);
    }
}
