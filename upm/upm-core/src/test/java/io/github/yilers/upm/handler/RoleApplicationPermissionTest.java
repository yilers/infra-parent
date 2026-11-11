package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.request.RolePermissionRequest;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleApplicationPermissionTest {
    private final RoleService roles = mock(RoleService.class);
    private final RolePermissionService grants = mock(RolePermissionService.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final RoleHandler handler = new RoleHandler(roles, mock(UserRoleService.class), grants,
            mock(RoleDeptService.class), permissions, mock(ApplicationHandler.class), mock(RoleAssignmentHandler.class));

    @Test
    void clearingOneScopeDoesNotDeleteOtherScopes() {
        when(roles.getOne(any())).thenReturn(new Role());
        Permission menu = new Permission();
        menu.setId(100L);
        menu.setUsable(1);
        when(permissions.findAllByDevice("web", 2L)).thenReturn(List.of(menu));
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole("platformAdmin")).thenReturn(true);
            handler.bindPermission(request(List.of()));
        }
        verify(grants).deleteByRoleIdAndPermissionIds(10L, List.of(100L));
        verify(grants, never()).deleteByRoleId(any());
        verify(grants, never()).deleteByRoleIdAndDevice(any(), any());
        verify(grants, never()).saveBatch(anyCollection());
    }

    @Test
    void rejectsForgedPermissionIdBeforeDeletingGrants() {
        when(roles.getOne(any())).thenReturn(new Role());
        when(permissions.findAllByDevice("web", 2L)).thenReturn(List.of());
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole("platformAdmin")).thenReturn(true);
            assertThrows(CommonException.class, () -> handler.bindPermission(request(List.of(999L))));
        }
        verify(grants).findPermissionListByRoleId(10L);
        verify(grants, never()).deleteByRoleIdAndPermissionIds(any(), any());
        verify(grants, never()).saveBatch(anyCollection());
    }

    @Test
    void existingOutOfScopeGrantIsPreservedWithoutBeingReinserted() {
        when(roles.getOne(any())).thenReturn(new Role());
        Permission manageable = permission(100L, 1);
        Permission disabled = permission(101L, 0);
        when(permissions.findAllByDevice("web", 2L)).thenReturn(List.of(manageable, disabled));
        when(grants.findPermissionListByRoleId(10L)).thenReturn(List.of(disabled));
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole("platformAdmin")).thenReturn(true);
            handler.bindPermission(request(List.of(101L)));
        }
        verify(grants).deleteByRoleIdAndPermissionIds(10L, List.of(100L));
        verify(grants, never()).saveBatch(anyCollection());
    }

    @Test
    void disabledPermissionCannotBeNewlyGrantedEvenByPlatformAdmin() {
        when(roles.getOne(any())).thenReturn(new Role());
        when(permissions.findAllByDevice("web", 2L)).thenReturn(List.of(permission(101L, 0)));
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole("platformAdmin")).thenReturn(true);
            assertThrows(CommonException.class, () -> handler.bindPermission(request(List.of(101L))));
        }
        verify(grants, never()).deleteByRoleIdAndPermissionIds(any(), any());
    }

    @Test
    void ordinaryUserUsesGrantablePermissionsOnly() {
        when(roles.getOne(any())).thenReturn(new Role());
        when(permissions.findGrantablePermissions(20L, "web", 2L)).thenReturn(List.of(permission(100L, 1)));
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole("platformAdmin")).thenReturn(false);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(20L);
            handler.bindPermission(request(List.of(100L)));
        }
        verify(permissions, never()).findPermissionsByUserId(anyLong(), anyString(), anyLong());
        verify(grants).deleteByRoleIdAndPermissionIds(10L, List.of(100L));
        verify(grants).saveBatch(anyCollection());
    }

    private Permission permission(Long id, int usable) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setAppId(2L);
        permission.setDevice("web");
        permission.setUsable(usable);
        return permission;
    }

    private RolePermissionRequest request(List<Long> ids) {
        RolePermissionRequest request = new RolePermissionRequest();
        request.setRoleId(10L);
        request.setAppId(2L);
        request.setDevice("web");
        request.setPermissionIdList(ids);
        return request;
    }
}
