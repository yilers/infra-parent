package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserRole;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.RoleDeptService;
import io.github.yilers.upm.service.PermissionService;
import io.github.yilers.upm.service.DeptLeaderService;
import io.github.yilers.upm.request.RoleUserRequest;
import io.github.yilers.upm.request.UserRequest;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证手工提交角色、用户 ID 时仍执行租户及授权范围校验。 */
class RoleAssignmentHandlerTest {
    private final RoleService roles = mock(RoleService.class);
    private final UserRoleService relations = mock(UserRoleService.class);
    private final UserService users = mock(UserService.class);
    private final CommonHandler common = mock(CommonHandler.class);
    private final RoleAssignmentHandler handler = new RoleAssignmentHandler(roles, relations, users, common);

    @BeforeEach
    void tenant() {
        RequestContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void rejectsPlatformRoleForNonPlatformOperatorButAllowsOrdinaryRole() {
        Role platform = role(10L, CommonConst.PLATFORM_ADMIN_ROLE_CODE);
        Role ordinary = role(20L, "employee");
        when(roles.listByIds(anyCollection())).thenReturn(List.of(platform, ordinary));
        try (var stp = mockStatic(StpUtil.class)) {
            assertThrows(CommonException.class, () -> handler.rolesForUser(null, List.of(10L)));
            assertEquals(List.of(20L), handler.rolesForUser(null, List.of(20L)));
            assertThrows(CommonException.class, () -> handler.checkRole(platform));
        }
    }

    @Test
    void platformOperatorStillCannotAssignForeignDisabledOrMissingRole() {
        Role platform = role(10L, CommonConst.PLATFORM_ADMIN_ROLE_CODE);
        Role foreign = role(20L, "employee");
        foreign.setTenantId(2L);
        Role disabled = role(30L, "disabled");
        disabled.setUsable(CommonConst.NO);
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(true);
            assertDoesNotThrow(() -> handler.checkRole(platform));
            assertThrows(CommonException.class, () -> handler.checkRole(foreign));
            assertThrows(CommonException.class, () -> handler.checkRole(disabled));
            assertThrows(CommonException.class, () -> handler.checkRole(null));
        }
    }

    @Test
    void editingUserPreservesProtectedAndDisabledRolesEvenWhenOmitted() {
        Role platform = role(10L, CommonConst.PLATFORM_ADMIN_ROLE_CODE);
        Role disabled = role(20L, "disabled");
        disabled.setUsable(CommonConst.NO);
        Role ordinary = role(30L, "employee");
        when(relations.list(org.mockito.ArgumentMatchers.<Wrapper<UserRole>>any()))
                .thenReturn(List.of(relation(10L), relation(20L), relation(30L)));
        when(roles.listByIds(anyCollection())).thenReturn(List.of(platform, disabled, ordinary));
        try (var stp = mockStatic(StpUtil.class)) {
            assertEquals(Set.of(10L, 20L), Set.copyOf(handler.rolesForUser(100L, List.of())));
        }
    }

    @Test
    void roleBindingRejectsForeignAndOutOfScopeUsersAndPreservesExistingOnes() {
        User visible = user(100L, 10L);
        User hidden = user(200L, 20L);
        User foreign = user(300L, 10L);
        foreign.setTenantId(2L);
        when(users.listByIds(anyCollection())).thenReturn(List.of(visible, hidden, foreign));
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
            when(common.findDataScopeByUserId(99L, null)).thenReturn(List.of(10L));
            assertThrows(CommonException.class, () -> handler.usersForRole(List.of(200L), List.of()));
            assertThrows(CommonException.class, () -> handler.usersForRole(List.of(300L), List.of()));
            assertEquals(Set.of(100L, 200L), Set.copyOf(handler.usersForRole(List.of(100L), List.of(200L))));
            when(common.findDataScopeByUserId(99L, null)).thenReturn(List.of());
            assertThrows(CommonException.class, () -> handler.usersForRole(List.of(100L), List.of()));
        }
    }

    @Test
    void forgedPlatformRoleIsRejectedByRoleBindingBeforeDeletingRelations() {
        when(roles.getOne(any())).thenReturn(role(10L, CommonConst.PLATFORM_ADMIN_ROLE_CODE));
        RoleHandler endpoint = new RoleHandler(roles, relations,
                mock(RolePermissionService.class), mock(RoleDeptService.class),
                mock(PermissionService.class), mock(ApplicationHandler.class), handler);
        var request = new RoleUserRequest();
        request.setRoleId(10L);
        request.setUserIdList(List.of(100L));
        try (var stp = mockStatic(StpUtil.class)) {
            assertThrows(CommonException.class, () -> endpoint.bindUser(request));
        }
        verifyNoInteractions(relations);
    }

    @Test
    void forgedPlatformRoleIsRejectedByUserCreationBeforeSavingUser() {
        when(roles.listByIds(anyCollection())).thenReturn(List.of(role(10L, CommonConst.PLATFORM_ADMIN_ROLE_CODE)));
        UserHandler endpoint = new UserHandler(users, relations,
                mock(RolePermissionService.class), common, mock(AuthHandler.class),
                mock(ApplicationAccessHandler.class), mock(LoginRiskHandler.class), mock(DeptLeaderHandler.class),
                mock(DeptLeaderService.class), handler);
        var request = new UserRequest();
        request.setAccount("test@example.com");
        request.setDeptId(10L);
        request.setRoleIdList(List.of(10L));
        try (var stp = mockStatic(StpUtil.class)) {
            assertThrows(CommonException.class, () -> endpoint.addUser(request));
        }
        verify(users, never()).save(any(User.class));
        verifyNoInteractions(relations);
    }

    @Test
    void forgedPlatformRoleIsRejectedByUserUpdateBeforeChangingUserOrRelations() {
        when(roles.listByIds(anyCollection())).thenReturn(List.of(role(10L, CommonConst.PLATFORM_ADMIN_ROLE_CODE)));
        when(users.getById(100L)).thenReturn(user(100L, 10L));
        UserHandler endpoint = new UserHandler(users, relations,
                mock(RolePermissionService.class), common, mock(AuthHandler.class),
                mock(ApplicationAccessHandler.class), mock(LoginRiskHandler.class), mock(DeptLeaderHandler.class),
                mock(DeptLeaderService.class), handler);
        UserRequest request = new UserRequest();
        request.setId(100L);
        request.setAccount("test@example.com");
        request.setDeptId(10L);
        request.setRoleIdList(List.of(10L));
        try (var stp = mockStatic(StpUtil.class)) {
            assertThrows(CommonException.class, () -> endpoint.updateUser(request));
        }
        verify(users, never()).updateById(any(User.class));
        verify(relations, never()).deleteByUserId(anyLong());
    }

    private Role role(Long id, String code) {
        Role role = new Role();
        role.setId(id);
        role.setTenantId(1L);
        role.setRoleCode(code);
        role.setUsable(CommonConst.YES);
        role.setDeleted(CommonConst.NO);
        return role;
    }

    private UserRole relation(Long roleId) {
        UserRole relation = new UserRole();
        relation.setUserId(100L);
        relation.setRoleId(roleId);
        return relation;
    }

    private User user(Long id, Long deptId) {
        User user = new User();
        user.setId(id);
        user.setTenantId(1L);
        user.setDeptId(deptId);
        user.setUsable(CommonConst.YES);
        user.setOperable(CommonConst.YES);
        user.setDeleted(CommonConst.NO);
        return user;
    }
}
