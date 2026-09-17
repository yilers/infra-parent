package io.github.yilers.upm.config;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.handler.ApplicationAccessHandler;
import io.github.yilers.web.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpmRequestContextConfigTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void loggedInRequestUsesAuthenticatedUserContext() throws Exception {
        ApplicationAccessHandler accessHandler = mock(ApplicationAccessHandler.class);
        User user = user(7L, 20L);
        when(accessHandler.currentUser()).thenReturn(user);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Tenant-Id")).thenReturn("30");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getLoginDeviceType).thenReturn("web");

            assertTrue(interceptor(accessHandler).preHandle(
                    request, mock(HttpServletResponse.class), new Object()));
        }

        assertEquals(7L, RequestContextHolder.getUserId());
        assertEquals(20L, RequestContextHolder.getTenantId());
        assertEquals("web", RequestContextHolder.getDeviceType());
        verify(accessHandler).currentApplication();
    }

    @Test
    void anonymousRequestDoesNotInitializeContext() throws Exception {
        ApplicationAccessHandler accessHandler = mock(ApplicationAccessHandler.class);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(false);

            assertTrue(interceptor(accessHandler).preHandle(
                    mock(HttpServletRequest.class), mock(HttpServletResponse.class), new Object()));
        }

        assertNull(RequestContextHolder.getUserId());
        assertNull(RequestContextHolder.getTenantId());
        verify(accessHandler, never()).currentUser();
        verify(accessHandler, never()).currentApplication();
    }

    private HandlerInterceptor interceptor(ApplicationAccessHandler accessHandler) {
        class Registry extends InterceptorRegistry {
            HandlerInterceptor first() {
                return ((MappedInterceptor) getInterceptors().getFirst()).getInterceptor();
            }
        }
        Registry registry = new Registry();
        new UpmRequestContextConfig(accessHandler).addInterceptors(registry);
        return registry.first();
    }

    private User user(Long id, Long tenantId) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        return user;
    }
}
