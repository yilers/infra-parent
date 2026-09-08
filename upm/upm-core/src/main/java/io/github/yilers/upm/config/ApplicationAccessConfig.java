package io.github.yilers.upm.config;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.handler.ApplicationAccessHandler;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ApplicationAccessConfig implements WebMvcConfigurer {
    private final ApplicationAccessHandler applicationAccessHandler;
    private final UserRoleService userRoleService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!StpUtil.isLogin()) {
                    return true;
                }
                User user = applicationAccessHandler.currentUser();
                Long targetTenantId = RequestContextHolder.getTenantId();
                RequestContextHolder.setTenantId(user.getTenantId());
                if (request.getHeader(CommonConst.HEADER_TENANT_ID) != null
                        && !"/user/current".equals(request.getServletPath())
                        && !user.getTenantId().equals(targetTenantId)) {
                    boolean platformAdmin = user.getTenantId() == 1L && userRoleService.findRoleListByUserId(user.getId()).stream()
                            .anyMatch(role -> CommonConst.YES.equals(role.getUsable())
                                    && CommonConst.PLATFORM_ADMIN_ROLE_CODE.equals(role.getRoleCode()));
                    if (!platformAdmin) {
                        throw new CommonException("无权操作其他租户");
                    }
                    RequestContextHolder.setTenantId(targetTenantId);
                }
                applicationAccessHandler.currentApplication();
                return true;
            }
        }).addPathPatterns("/**").excludePathPatterns("/auth/**", "/error", "/public/**", "/actuator/**")
                .order(-100);
    }
}
