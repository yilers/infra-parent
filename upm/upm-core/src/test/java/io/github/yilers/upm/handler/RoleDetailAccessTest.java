package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleDetailAccessTest {
    @Test
    void hiddenPlatformRoleCannotBeReadByIdAndUnknownRoleDoesNotLeakRelations() {
        RoleService roles = mock(RoleService.class);
        UserRoleService users = mock(UserRoleService.class);
        RolePermissionService permissions = mock(RolePermissionService.class);
        RoleHandler handler = new RoleHandler(roles, users, permissions, mock(RoleDeptService.class),
                mock(PermissionService.class), mock(ApplicationHandler.class), mock(RoleAssignmentHandler.class));
        Role platform = new Role();
        platform.setId(10L);
        platform.setRoleCode("platformAdmin");
        when(roles.getById(10L)).thenReturn(platform);
        try (var login = mockStatic(StpUtil.class)) {
            assertThrows(CommonException.class, () -> handler.findById(10L));
            assertThrows(CommonException.class, () -> handler.findById(999L));
        }
        verifyNoInteractions(users, permissions);
    }
}
