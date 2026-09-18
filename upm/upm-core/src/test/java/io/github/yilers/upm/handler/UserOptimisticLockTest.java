package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.UserRequest;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserOptimisticLockTest {
    private final UserService userService = mock(UserService.class);
    private final UserRoleService userRoleService = mock(UserRoleService.class);
    private final CommonHandler commonHandler = mock(CommonHandler.class);
    private final AuthHandler authHandler = mock(AuthHandler.class);
    private final UserHandler handler = new UserHandler(userService, userRoleService,
            mock(RolePermissionService.class), commonHandler, authHandler, mock(ApplicationAccessHandler.class));

    @Test
    void updateUsesClientVersionAndReportsConflict() {
        User user = user(10L, 2);
        when(userService.getById(10L)).thenReturn(user);
        when(userService.findByAccount("user@yilers.com")).thenReturn(user);
        when(userService.updateById(any())).thenReturn(false);
        UserRequest request = request();

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            assertThrows(CommonException.class, () -> handler.updateUser(request));
        }

        verify(commonHandler).checkDataScope(99L, 30L);
        verify(userRoleService, never()).deleteByUserId(any());
    }

    @Test
    void failedDisableDoesNotLogoutUser() {
        User user = user(10L, 2);
        when(userService.getById(10L)).thenReturn(user);
        when(userService.updateById(user)).thenReturn(false);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            assertThrows(CommonException.class, () -> handler.usable(operate(10L)));
        }

        verify(authHandler, never()).logout(10L);
    }

    private UserRequest request() {
        UserRequest request = new UserRequest();
        request.setId(10L);
        request.setAccount("user@yilers.com");
        request.setDeptId(30L);
        request.setRoleIdList(List.of(1L));
        request.setVersion(1);
        return request;
    }

    private User user(Long id, Integer version) {
        User user = new User();
        user.setId(id);
        user.setAccount("user@yilers.com");
        user.setDeptId(20L);
        user.setOperable(1);
        user.setUsable(1);
        user.setVersion(version);
        return user;
    }

    private io.github.yilers.api.base.BaseOperateRequest operate(Long id) {
        io.github.yilers.api.base.BaseOperateRequest request = new io.github.yilers.api.base.BaseOperateRequest();
        request.setId(id);
        return request;
    }
}
