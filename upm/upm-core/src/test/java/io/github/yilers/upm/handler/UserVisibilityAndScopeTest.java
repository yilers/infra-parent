package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.UserDataScopeRequest;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserVisibilityAndScopeTest {
    private final UserService users = mock(UserService.class);
    private final UserDataScopeService scopes = mock(UserDataScopeService.class);
    private final CommonHandler common = mock(CommonHandler.class);
    private final UserDataScopeHandler scopeHandler = new UserDataScopeHandler(users, scopes, common);

    @AfterEach
    void clear() { RequestContextHolder.clear(); }

    @Test
    void clientCannotEnablePlatformUserVisibility() {
        UserHandler handler = new UserHandler(users, mock(UserRoleService.class), mock(RolePermissionService.class),
                common, mock(AuthHandler.class), mock(ApplicationAccessHandler.class), mock(LoginRiskHandler.class),
                mock(DeptLeaderHandler.class), mock(DeptLeaderService.class), mock(RoleAssignmentHandler.class));
        when(users.findByPage(any(), any())).thenReturn(new Page<>());
        try (var login = mockStatic(StpUtil.class)) {
            login.when(() -> StpUtil.getRoleList(null)).thenReturn(List.of());
            for (Integer forged : new Integer[]{1, 2, null}) {
                BasePageRequest<UserPageRequest> request = new BasePageRequest<>();
                UserPageRequest condition = new UserPageRequest();
                condition.setLookPlatformUser(forged);
                request.setData(condition);
                handler.page(request);
                assertEquals(0, condition.getLookPlatformUser());
            }
        }
    }

    @Test
    void foreignOrOutOfScopeUserCannotHaveScopeReadOrOverwritten() {
        RequestContextHolder.setTenantId(1L);
        User target = user();
        target.setTenantId(2L);
        when(users.getById(10L)).thenReturn(target);
        assertThrows(CommonException.class, () -> scopeHandler.findByUserId(10L));
        assertThrows(CommonException.class, () -> scopeHandler.bind(request()));
        target.setTenantId(1L);
        try (var login = mockStatic(StpUtil.class)) {
            login.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            doThrow(new CommonException("越权操作")).when(common).checkDataScope(99L, 20L);
            assertThrows(CommonException.class, () -> scopeHandler.findByUserId(10L));
            assertThrows(CommonException.class, () -> scopeHandler.bind(request()));
        }
        verifyNoInteractions(scopes);
    }

    @Test
    void protectedUserCannotBeChangedButManageableUserRetainsOverrideSemantics() {
        RequestContextHolder.setTenantId(1L);
        User target = user();
        target.setOperable(0);
        when(users.getById(10L)).thenReturn(target);
        try (var login = mockStatic(StpUtil.class)) {
            assertThrows(CommonException.class, () -> scopeHandler.bind(request()));
            verifyNoInteractions(scopes);
            target.setOperable(1);
            UserDataScopeRequest request = request();
            scopeHandler.bind(request);
            verify(scopes).bind(request);
        }
    }

    private User user() {
        User user = new User();
        user.setId(10L);
        user.setTenantId(1L);
        user.setDeptId(20L);
        user.setOperable(1);
        return user;
    }

    private UserDataScopeRequest request() {
        UserDataScopeRequest request = new UserDataScopeRequest();
        request.setUserId(10L);
        request.setScopes(List.of());
        return request;
    }
}
