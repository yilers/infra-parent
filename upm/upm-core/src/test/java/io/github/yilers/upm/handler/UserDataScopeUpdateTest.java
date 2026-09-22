package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.UserRequest;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserDataScopeUpdateTest {
    private final UserService userService = mock(UserService.class);
    private final UserRoleService userRoleService = mock(UserRoleService.class);
    private final CommonHandler commonHandler = mock(CommonHandler.class);
    private final UserHandler handler = new UserHandler(userService, userRoleService,
            mock(RolePermissionService.class), commonHandler, mock(AuthHandler.class),
            mock(ApplicationAccessHandler.class));

    private User existingUser() {
        User user = new User();
        user.setId(10L);
        user.setDeptId(20L);
        user.setOperable(1);
        when(userService.getById(10L)).thenReturn(user);
        return user;
    }

    private UserRequest request(Long deptId) {
        UserRequest request = new UserRequest();
        request.setId(10L);
        request.setAccount("user@yilers.com");
        request.setDeptId(deptId);
        request.setRoleIdList(List.of(1L));
        request.setVersion(1);
        return request;
    }

    @Test
    void rejectsOriginalDepartmentOutsideScopeEvenWhenTargetIsAllowed() {
        User user = existingUser();
        doThrow(new CommonException("越权操作")).when(commonHandler).checkDataScope(99L, 20L);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            assertThrows(CommonException.class, () -> handler.updateUser(request(30L)));
        }

        verify(commonHandler, never()).checkDataScope(99L, 30L);
        verify(userService, never()).updateById(any());
        verifyNoInteractions(userRoleService);
        assertEquals(20L, user.getDeptId());
    }

    @Test
    void rejectsTargetDepartmentOutsideScope() {
        User user = existingUser();
        doThrow(new CommonException("越权操作")).when(commonHandler).checkDataScope(99L, 30L);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            assertThrows(CommonException.class, () -> handler.updateUser(request(30L)));
        }

        var order = inOrder(commonHandler);
        order.verify(commonHandler).checkDataScope(99L, 20L);
        order.verify(commonHandler).checkDataScope(99L, 30L);
        verify(userService, never()).updateById(any());
        verifyNoInteractions(userRoleService);
        assertEquals(20L, user.getDeptId());
    }

    @Test
    void unchangedDepartmentIsCheckedOnceBeforeUpdating() {
        User user = existingUser();
        when(userService.updateById(user)).thenReturn(true);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            handler.updateUser(request(20L));
        }

        var order = inOrder(commonHandler, userService);
        order.verify(commonHandler).checkDataScope(99L, 20L);
        order.verify(userService).updateById(user);
        verifyNoMoreInteractions(commonHandler);
        verify(userService).cleanCache(10L);
    }

    @Test
    void allowsTransferAfterCheckingBothDepartments() {
        User user = existingUser();
        when(userService.updateById(user)).thenReturn(true);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            handler.updateUser(request(30L));
        }

        var order = inOrder(commonHandler, userService);
        order.verify(commonHandler).checkDataScope(99L, 20L);
        order.verify(commonHandler).checkDataScope(99L, 30L);
        order.verify(userService).updateById(user);
        assertEquals(30L, user.getDeptId());
        verify(userRoleService).saveUserRoleRelation(10L, List.of(1L));
        verify(userService).cleanCache(10L);
    }
}
