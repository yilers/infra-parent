package io.github.yilers.infra.flow;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** 核验流程候选人展开不会把其他租户或停用账号固化进任务。 */
class UpmWorkflowIdentityProviderTest {

    private final UserService users = mock(UserService.class);
    private final UserRoleService userRoles = mock(UserRoleService.class);
    private final RoleService roles = mock(RoleService.class);
    private final DeptService depts = mock(DeptService.class);
    private final UpmWorkflowIdentityProvider provider = new UpmWorkflowIdentityProvider(users, userRoles, roles, depts);

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void resolvesRoleUsersAndFiltersDisabledOrForeignAccounts() {
        try (var ignored = mockStatic(StpUtil.class)) {
            login();
            Role role = new Role();
            role.setId(20L);
            role.setTenantId(1L);
            role.setUsable(1);
            when(roles.getById(20L)).thenReturn(role);
            when(userRoles.findUserIdListByRoleId(20L)).thenReturn(List.of(10L, 11L, 12L));
            when(users.listByIds(anyCollection())).thenReturn(List.of(user(10L, 1L, 1),
                    user(11L, 1L, 0), user(12L, 2L, 1)));

            assertEquals(List.of("10"), provider.resolveUserIds(List.of("user:10", "role:20")));
        }
    }

    @Test
    void rejectsDisabledRolesAndForeignDepartments() {
        try (var ignored = mockStatic(StpUtil.class)) {
            login();
            Role role = new Role();
            role.setTenantId(1L);
            role.setUsable(0);
            when(roles.getById(20L)).thenReturn(role);
            assertThrows(IllegalArgumentException.class, () -> provider.resolveUserIds(List.of("role:20")));
            Dept dept = new Dept();
            dept.setTenantId(2L);
            dept.setUsable(1);
            when(depts.getById(30L)).thenReturn(dept);
            assertThrows(IllegalArgumentException.class, () -> provider.resolveUserIds(List.of("dept:30")));
        }
    }

    @Test
    void rejectsMalformedHandlerTypes() {
        try (var ignored = mockStatic(StpUtil.class)) {
            login();
            assertThrows(IllegalArgumentException.class, () -> provider.resolveUserIds(List.of("unknown:10")));
            assertThrows(IllegalArgumentException.class, () -> provider.resolveUserIds(List.of("user:abc")));
        }
    }

    private void login() {
        when(StpUtil.getLoginIdAsLong()).thenReturn(10L);
        RequestContextHolder.setTenantId(1L);
        when(users.getById(10L)).thenReturn(user(10L, 1L, 1));
    }

    private User user(Long id, Long tenantId, int usable) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsable(usable);
        return user;
    }
}
