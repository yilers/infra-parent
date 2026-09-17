package io.github.yilers.upm.config;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.handler.ApplicationAccessHandler;
import io.github.yilers.web.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * UPM 请求上下文配置。
 *
 * <p>登录用户、所属租户和登录终端均从服务端登录态获取，不信任客户端提交的身份上下文。</p>
 */
@Configuration
@RequiredArgsConstructor
public class UpmRequestContextConfig implements WebMvcConfigurer {
    private final ApplicationAccessHandler applicationAccessHandler;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(@NotNull HttpServletRequest request,
                                     @NotNull HttpServletResponse response,
                                     @NotNull Object handler) {
                if (!StpUtil.isLogin()) {
                    return true;
                }
                User user = applicationAccessHandler.currentUser();
                RequestContextHolder.setUserId(user.getId());
                RequestContextHolder.setTenantId(user.getTenantId());
                RequestContextHolder.setDeviceType(StpUtil.getLoginDeviceType());
                applicationAccessHandler.currentApplication();
                return true;
            }
        }).addPathPatterns("/**")
                .excludePathPatterns("/auth/**", "/error", "/public/**", "/actuator/**")
                .order(-100);
    }
}
