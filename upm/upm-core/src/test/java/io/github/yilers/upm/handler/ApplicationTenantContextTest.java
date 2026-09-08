package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.config.ApplicationAccessConfig;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationTenantContextTest {
    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void tenantLocalPlatformAdminCannotForgeAnotherTenant() throws Exception {
        assertTenantSelection(20L, 30L, "/application/findAll", false);
    }

    @Test
    void tenantOnePlatformAdminCanManageSelectedTenant() throws Exception {
        assertTenantSelection(1L, 20L, "/application/findAll", true);
    }

    @Test
    void bootstrapUserInfoUsesOwnTenantEvenWithDefaultHeader() throws Exception {
        assertTenantSelection(20L, 1L, "/user/current", true);
        assertEquals(20L, RequestContextHolder.getTenantId());
    }

    private void assertTenantSelection(Long ownTenant, Long targetTenant, String path, boolean allowed) throws Exception {
        ApplicationAccessHandler access = mock(ApplicationAccessHandler.class);
        UserRoleService roles = mock(UserRoleService.class);
        User user = new User();
        user.setId(7L);
        user.setTenantId(ownTenant);
        when(access.currentUser()).thenReturn(user);
        Role role = new Role();
        role.setRoleCode(CommonConst.PLATFORM_ADMIN_ROLE_CODE);
        role.setUsable(1);
        when(roles.findRoleListByUserId(7L)).thenReturn(List.of(role));
        class Registry extends InterceptorRegistry {
            HandlerInterceptor interceptor() {
                return ((MappedInterceptor) getInterceptors().getFirst()).getInterceptor();
            }
        }
        Registry registry = new Registry();
        new ApplicationAccessConfig(access, roles).addInterceptors(registry);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(CommonConst.HEADER_TENANT_ID)).thenReturn(targetTenant.toString());
        when(request.getServletPath()).thenReturn(path);
        RequestContextHolder.setTenantId(targetTenant);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            if (allowed) {
                assertTrue(registry.interceptor().preHandle(request, mock(HttpServletResponse.class), new Object()));
                verify(access).currentApplication();
            } else {
                assertThrows(CommonException.class, () -> registry.interceptor().preHandle(request, mock(HttpServletResponse.class), new Object()));
                verify(access, never()).currentApplication();
            }
        }
    }
}
