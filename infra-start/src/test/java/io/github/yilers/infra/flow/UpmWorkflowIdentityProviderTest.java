package io.github.yilers.infra.flow;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.DeptLeader;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.DeptLeaderService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
    private final DeptLeaderService leaders = mock(DeptLeaderService.class);
    private final UpmWorkflowIdentityProvider provider = new UpmWorkflowIdentityProvider(users, userRoles, roles, depts, leaders);

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

    @Test
    void resolvesDisplayNamesWithoutLeakingForeignTenantOrInvalidFlags() {
        try (var ignored = mockStatic(StpUtil.class)) {
            login();
            Role role = new Role();
            role.setTenantId(1L);
            role.setRoleName("租户管理员");
            when(roles.getById(20L)).thenReturn(role);
            Dept dept = new Dept();
            dept.setTenantId(1L);
            dept.setDeptName("开发部");
            when(depts.getById(30L)).thenReturn(dept);
            User employee = user(11L, 1L, 1);
            employee.setName("张三");
            when(users.getById(11L)).thenReturn(employee);
            Role foreign = new Role();
            foreign.setTenantId(2L);
            foreign.setRoleName("其他租户的角色");
            when(roles.getById(21L)).thenReturn(foreign);

            assertEquals(Map.of("role:20", "租户管理员", "dept:30", "开发部", "user:11", "张三"),
                    provider.resolvePermissionNames(List.of("role:20", "role:20", "dept:30", "user:11",
                            "role:21", "role:404", "user:abc", "role:9999999999999999999", "unknown:10")));
        }
    }

    private void login() {
        when(StpUtil.getLoginIdAsLong()).thenReturn(10L);
        RequestContextHolder.setTenantId(1L);
        when(users.getById(10L)).thenReturn(user(10L, 1L, 1));
    }

    @Test
    void dynamicRulesPreferDeputiesThenFallbackAndExcludeApplicant() {
        try (var ignored = mockStatic(StpUtil.class)) {
            login();
            User applicant = user(10L, 1L, 1);
            applicant.setDeptId(100L);
            when(users.getById(10L)).thenReturn(applicant);
            Dept dept = department(100L, 200L, 1L);
            when(depts.getById(100L)).thenReturn(dept);
            when(depts.getById(200L)).thenReturn(department(200L, null, 1L));
            when(users.listByIds(anyCollection())).thenReturn(List.of(user(20L, 1L, 1), user(21L, 1L, 1),
                    user(30L, 1L, 1), user(40L, 1L, 1), user(50L, 2L, 1), user(60L, 1L, 0)));
            when(leaders.findCurrentByDeptId(100L)).thenReturn(List.of(leader(100L, 10L, 2), leader(100L, 20L, 2),
                    leader(100L, 21L, 2), leader(100L, 30L, 1), leader(100L, 50L, 2), leader(100L, 60L, 2)));
            when(leaders.findCurrentByDeptId(200L)).thenReturn(List.of(leader(200L, 40L, 1)));
            assertEquals(List.of("10"), provider.resolveDynamicAssignees("10", WorkflowAssigneeRule.APPLICANT));
            assertEquals(List.of("20", "21"), provider.resolveDynamicAssignees("10", WorkflowAssigneeRule.DEPARTMENT_APPROVERS));
            assertEquals(List.of("40"), provider.resolveDynamicAssignees("10", WorkflowAssigneeRule.PARENT_DEPARTMENT_APPROVERS));
            when(leaders.findCurrentByDeptId(100L)).thenReturn(List.of(leader(100L, 10L, 2), leader(100L, 30L, 1)));
            assertEquals(List.of("30"), provider.resolveDynamicAssignees("10", WorkflowAssigneeRule.DEPARTMENT_APPROVERS));
            when(leaders.findCurrentByDeptId(100L)).thenReturn(List.of(leader(100L, 10L, 1)));
            assertThrows(CommonException.class, () -> provider.resolveDynamicAssignees("10", WorkflowAssigneeRule.DEPARTMENT_APPROVERS));
            assertThrows(CommonException.class, () -> provider.resolveDynamicAssignees("99", WorkflowAssigneeRule.APPLICANT));
            when(depts.getById(200L)).thenReturn(department(200L, null, 2L));
            assertThrows(CommonException.class, () -> provider.resolveDynamicAssignees("10", WorkflowAssigneeRule.PARENT_DEPARTMENT_APPROVERS));
            dept.setParentId(null);
            assertThrows(CommonException.class, () -> provider.resolveDynamicAssignees("10", WorkflowAssigneeRule.PARENT_DEPARTMENT_APPROVERS));
        }
    }

    private Dept department(Long id, Long parentId, Long tenantId) {
        Dept dept = new Dept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setTenantId(tenantId);
        dept.setUsable(1);
        return dept;
    }

    private DeptLeader leader(Long deptId, Long userId, int type) {
        DeptLeader leader = new DeptLeader();
        leader.setDeptId(deptId);
        leader.setUserId(userId);
        leader.setLeaderType(type);
        leader.setTenantId(1L);
        return leader;
    }

    private User user(Long id, Long tenantId, int usable) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsable(usable);
        return user;
    }
}
