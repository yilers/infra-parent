package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLoginUnlockTest {

    private final UserService userService = mock(UserService.class);
    private final CommonHandler commonHandler = mock(CommonHandler.class);
    private final LoginRiskHandler loginRiskHandler = mock(LoginRiskHandler.class);
    private final UserHandler handler = new UserHandler(userService, mock(UserRoleService.class),
            mock(RolePermissionService.class), commonHandler, mock(AuthHandler.class),
            mock(ApplicationAccessHandler.class), loginRiskHandler);

    @Test
    void clearsLoginRiskStateForVisibleUser() {
        User user = new User();
        user.setId(10L);
        user.setDeptId(20L);
        when(userService.getById(10L)).thenReturn(user);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            handler.unlockLogin(10L);
        }

        verify(commonHandler).checkDataScope(99L, 20L);
        verify(loginRiskHandler).clear(user);
    }

    @Test
    void rejectsUnknownUser() {
        assertThrows(CommonException.class, () -> handler.unlockLogin(10L));

        verify(loginRiskHandler, never()).clear(org.mockito.ArgumentMatchers.any());
    }
}
