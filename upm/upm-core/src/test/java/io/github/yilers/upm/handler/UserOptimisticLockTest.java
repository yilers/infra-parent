package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.crypto.digest.BCrypt;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.UserRequest;
import io.github.yilers.upm.request.UserUpdatePwdRequest;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void passwordChangeClearsInitialFlagAndUserCache() {
        User user = user(10L, 2);
        user.setPassword(BCrypt.hashpw("old-password"));
        user.setExpand("{\"initPwd\":true,\"theme\":\"dark\"}");
        when(userService.getById(10L)).thenReturn(user);
        when(userService.updateById(any())).thenReturn(true);
        UserUpdatePwdRequest request = new UserUpdatePwdRequest();
        request.setUserId(10L);
        request.setOldPwd("old-password");
        request.setNewPwd("new-password");

        handler.updatePwd(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(captor.capture());
        assertFalse(UserExpandHelper.isInitPwd(captor.getValue().getExpand()));
        assertTrue(captor.getValue().getExpand().contains("\"theme\":\"dark\""));
        verify(userService).cleanCache(10L);
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
