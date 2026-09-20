package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.api.base.BaseOperateRequest;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.request.RoleUserRequest;
import io.github.yilers.upm.service.PermissionService;
import io.github.yilers.upm.service.RoleDeptService;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleCacheInvalidationTest {
    private final RoleService roleService = mock(RoleService.class);
    private final UserRoleService userRoleService = mock(UserRoleService.class);
    private final RoleHandler handler = new RoleHandler(roleService, userRoleService,
            mock(RolePermissionService.class), mock(RoleDeptService.class), mock(PermissionService.class),
            mock(ApplicationHandler.class));

    @Test
    void changingRoleStatusClearsAllRelatedUserRoleCaches() {
        Role role = new Role();
        role.setId(10L);
        role.setOperable(1);
        role.setUsable(1);
        when(roleService.getById(10L)).thenReturn(role);
        when(roleService.updateById(role)).thenReturn(true);
        when(userRoleService.findUserIdListByRoleId(10L)).thenReturn(List.of(100L, 200L));
        BaseOperateRequest request = new BaseOperateRequest();
        request.setId(10L);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole("platformAdmin")).thenReturn(true);
            handler.usable(request);
        }

        verify(userRoleService).cleanCache(100L);
        verify(userRoleService).cleanCache(200L);
    }

    @Test
    void rebindingRoleClearsRemovedAndAddedUserCaches() {
        when(userRoleService.findUserIdListByRoleId(10L)).thenReturn(List.of(100L, 200L));
        RoleUserRequest request = new RoleUserRequest();
        request.setRoleId(10L);
        request.setUserIdList(List.of(200L, 300L));

        handler.bindUser(request);

        verify(userRoleService).deleteByRoleId(10L);
        verify(userRoleService).saveBatch(anyCollection());
        verify(userRoleService).cleanCache(100L);
        verify(userRoleService).cleanCache(200L);
        verify(userRoleService).cleanCache(300L);
    }
}
