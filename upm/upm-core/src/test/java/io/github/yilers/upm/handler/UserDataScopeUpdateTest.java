package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.UserRequest;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.DeptLeaderService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import io.github.yilers.web.field.FieldPermissionService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserDataScopeUpdateTest {
    private final UserService userService = mock(UserService.class);
    private final UserRoleService userRoleService = mock(UserRoleService.class);
    private final CommonHandler commonHandler = mock(CommonHandler.class);
    private final RoleAssignmentHandler roleAssignmentHandler = mock(RoleAssignmentHandler.class);
    private final UserHandler handler = new UserHandler(userService, userRoleService,
            mock(RolePermissionService.class), commonHandler, mock(AuthHandler.class),
            mock(ApplicationAccessHandler.class), mock(LoginRiskHandler.class),
            mock(DeptLeaderHandler.class), mock(DeptLeaderService.class), roleAssignmentHandler);

    private final FieldPermissionService fields = mock(FieldPermissionService.class);

    @BeforeEach
    void fieldChecks() throws Exception {
        var field = UserHandler.class.getDeclaredField("fieldPermissions");
        field.setAccessible(true);
        field.set(handler, fields);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "user-fields"), User.class);
    }

    @Test
    void omittedContactFieldsKeepOriginalValues() {
        User user = existingUser();
        user.setPhone("13812345678");
        user.setEmail("a@example.com");
        when(userService.updateById(user)).thenReturn(true);
        try (var stp = mockStatic(StpUtil.class)) {
            handler.updateUser(request(20L));
        }
        assertEquals("13812345678", user.getPhone());
        assertEquals("a@example.com", user.getEmail());
        verifyNoInteractions(fields);
    }

    @Test
    void explicitClearIsWrittenAndCheckedBeforeDatabaseUpdate() {
        User user = existingUser();
        user.setPhone("13812345678");
        UserRequest request = request(20L);
        request.setPhone(null);
        when(userService.update(eq(user), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenAnswer(call -> {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User> wrapper = call.getArgument(1);
            org.junit.jupiter.api.Assertions.assertTrue(wrapper.getSqlSet().contains("phone="));
            org.junit.jupiter.api.Assertions.assertTrue(wrapper.getParamNameValuePairs().containsValue(null));
            return true;
        });
        try (var stp = mockStatic(StpUtil.class)) {
            handler.updateUser(request);
        }
        verify(fields).checkUpdate("system:user:phone", "手机号", true, "13812345678", null);
        verify(userService, never()).updateById(any());
    }

    @Test
    void unauthorizedContactChangeDoesNotReachDatabase() {
        User user = existingUser();
        user.setPhone("13812345678");
        UserRequest request = request(20L);
        request.setPhone("13912345678");
        doThrow(new CommonException("禁止修改")).when(fields)
                .checkUpdate("system:user:phone", "手机号", true, "13812345678", "13912345678");
        try (var stp = mockStatic(StpUtil.class)) {
            assertThrows(CommonException.class, () -> handler.updateUser(request));
        }
        assertEquals("13812345678", user.getPhone());
        verify(userService, never()).updateById(any());
        verify(userRoleService, never()).deleteByUserId(any());
    }

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
        when(roleAssignmentHandler.rolesForUser(10L, List.of(1L))).thenReturn(List.of(1L));
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
