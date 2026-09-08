package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationAccessHandlerTest {
    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void scopesByServerApplicationAndLoginDeviceAndRestoresTenant() {
        ApplicationService applications = mock(ApplicationService.class);
        UserService users = mock(UserService.class);
        UserRoleService roles = mock(UserRoleService.class);
        RolePermissionService permissions = mock(RolePermissionService.class);
        ApplicationAccessHandler handler = new ApplicationAccessHandler(applications, users, roles, permissions);
        User user = new User();
        user.setTenantId(1L);
        user.setUsable(1);
        when(users.getById(7L)).thenReturn(user);
        Application application = new Application();
        application.setId(1L);
        application.setUsable(1);
        when(applications.getOne(any())).thenReturn(application);
        Role role = new Role();
        role.setId(10L);
        role.setUsable(1);
        when(roles.findRoleListByUserId(7L)).thenReturn(List.of(role));
        Permission allowed = permission(1L, "web", 1);
        Permission otherApplication = permission(2L, "web", 1);
        Permission otherDevice = permission(1L, "app", 1);
        Permission disabled = permission(1L, "web", 0);
        when(permissions.findPermissionListByRoleIdList(List.of(10L)))
                .thenReturn(List.of(allowed, otherApplication, otherDevice, disabled));
        RequestContextHolder.setTenantId(20L);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            stp.when(StpUtil::getLoginDeviceType).thenReturn("web");
            assertEquals(List.of(allowed), handler.currentPermissions(7L));
            assertEquals(20L, RequestContextHolder.getTenantId());
            application.setUsable(0);
            assertThrows(CommonException.class, () -> handler.currentPermissions(7L));
            assertEquals(20L, RequestContextHolder.getTenantId());
        }
    }

    private Permission permission(Long appId, String device, int usable) {
        Permission permission = new Permission();
        permission.setAppId(appId);
        permission.setDevice(device);
        permission.setUsable(usable);
        permission.setPermissionCode("system:user:add");
        return permission;
    }
}
