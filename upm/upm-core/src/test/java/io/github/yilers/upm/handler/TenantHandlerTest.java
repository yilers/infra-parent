package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantHandlerTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void deletesTenantAndLogsOutTenantUsers() {
        TenantService tenants = mock(TenantService.class);
        UserService users = mock(UserService.class);
        AuthHandler authHandler = mock(AuthHandler.class);
        TenantHandler handler = new TenantHandler(tenants, users, authHandler);

        Tenant tenant = new Tenant();
        tenant.setId(5L);
        tenant.setOperable(CommonConst.YES);
        when(tenants.getById(5L)).thenReturn(tenant);
        when(tenants.removeById(5L)).thenReturn(true);
        when(users.list()).thenAnswer(invocation -> {
            assertEquals(5L, RequestContextHolder.getTenantId());
            User first = new User();
            first.setId(101L);
            User second = new User();
            second.setId(102L);
            return List.of(first, second);
        });

        RequestContextHolder.setTenantId(CommonConst.PLATFORM_TENANT_ID);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(true);
            handler.deleteById(5L);
        }

        verify(tenants).removeById(5L);
        verify(authHandler).logout(101L);
        verify(authHandler).logout(102L);
        assertEquals(CommonConst.PLATFORM_TENANT_ID, RequestContextHolder.getTenantId());
    }

    @Test
    void refusesToDeletePlatformTenant() {
        TenantHandler handler = new TenantHandler(
                mock(TenantService.class), mock(UserService.class), mock(AuthHandler.class));
        RequestContextHolder.setTenantId(CommonConst.PLATFORM_TENANT_ID);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(true);
            CommonException exception = assertThrows(CommonException.class,
                    () -> handler.deleteById(CommonConst.PLATFORM_TENANT_ID));
            assertEquals("平台租户不能删除", exception.getMessage());
        }
    }
}
