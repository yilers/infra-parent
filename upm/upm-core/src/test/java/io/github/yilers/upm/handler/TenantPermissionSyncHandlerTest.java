package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Application;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.RolePermission;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.response.TenantPermissionSyncResponse;
import io.github.yilers.upm.service.ApplicationService;
import io.github.yilers.upm.service.PermissionService;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.web.context.RequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantPermissionSyncHandlerTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void synchronizesTemplateDataAndIsIdempotent() {
        TenantService tenants = mock(TenantService.class);
        ApplicationService applications = mock(ApplicationService.class);
        PermissionService permissions = mock(PermissionService.class);
        RoleService roles = mock(RoleService.class);
        RolePermissionService rolePermissions = mock(RolePermissionService.class);
        UserRoleService userRoles = mock(UserRoleService.class);
        TenantPermissionSyncHandler handler = new TenantPermissionSyncHandler(
                tenants, applications, permissions, roles, rolePermissions, userRoles);

        when(tenants.getById(2L)).thenReturn(new Tenant());
        Application templateApplication = application(1L, 1L, "infra", "基础管理平台");
        Application targetApplication = application(2L, 2L, "infra", "旧应用名称");
        targetApplication.setUsable(CommonConst.NO);
        List<Application> targetApplications = new ArrayList<>(List.of(targetApplication));
        when(applications.list()).thenAnswer(invocation -> CommonConst.PLATFORM_TENANT_ID
                .equals(RequestContextHolder.getTenantId()) ? List.of(templateApplication) : targetApplications);
        AtomicLong applicationId = new AtomicLong(20L);
        when(applications.save(any())).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            application.setId(applicationId.incrementAndGet());
            targetApplications.add(application);
            return true;
        });
        when(applications.updateById(any())).thenReturn(true);

        Permission templateRoot = permission(100L, null, 0L, 1L, "系统管理");
        Permission templateChild = permission(101L, null, 100L, 1L, "用户管理");
        Permission targetRoot = permission(200L, 100L, 0L, 2L, "旧系统管理");
        Permission stale = permission(300L, 999L, 0L, 2L, "已删除模板菜单");
        List<Permission> targetPermissions = new ArrayList<>(List.of(targetRoot, stale));
        when(permissions.list()).thenAnswer(invocation -> CommonConst.PLATFORM_TENANT_ID
                .equals(RequestContextHolder.getTenantId())
                ? List.of(templateRoot, templateChild) : targetPermissions);
        when(permissions.saveBatch(anyCollection())).thenAnswer(invocation -> {
            targetPermissions.addAll(invocation.<Collection<Permission>>getArgument(0));
            return true;
        });
        when(permissions.updateBatchById(anyCollection())).thenReturn(true);

        Role templateTenantAdmin = role(20L);
        Role targetTenantAdmin = role(220L);
        when(roles.findByRoleCode(CommonConst.TENANT_ADMIN_ROLE_CODE)).thenAnswer(invocation ->
                CommonConst.PLATFORM_TENANT_ID.equals(RequestContextHolder.getTenantId())
                        ? templateTenantAdmin : targetTenantAdmin);
        Set<Long> targetGrantIds = new LinkedHashSet<>(List.of(200L, 300L));
        when(rolePermissions.findPermissionListByRoleId(20L)).thenReturn(List.of(templateRoot, templateChild));
        when(rolePermissions.findPermissionListByRoleId(220L)).thenAnswer(invocation -> targetPermissions.stream()
                .filter(permission -> targetGrantIds.contains(permission.getId())).toList());
        when(rolePermissions.saveBatch(anyCollection())).thenAnswer(invocation -> {
            invocation.<Collection<RolePermission>>getArgument(0).forEach(relation ->
                    targetGrantIds.add(relation.getPermissionId()));
            return true;
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            targetGrantIds.removeAll(invocation.<List<Long>>getArgument(1));
            return null;
        }).when(rolePermissions).deleteByRoleIdAndPermissionIds(any(), any());
        when(userRoles.findUserIdListByRoleId(220L)).thenReturn(List.of(9L));

        RequestContextHolder.setTenantId(CommonConst.PLATFORM_TENANT_ID);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(true);

            assertEquals(CommonConst.NO, targetApplication.getUsable());
            TenantPermissionSyncResponse preview = handler.preview(2L);
            assertEquals(CommonConst.NO, targetApplication.getUsable());
            assertEquals(0, preview.getApplicationAddCount());
            assertEquals(1, preview.getApplicationUpdateCount());
            assertEquals(1, preview.getPermissionAddCount());
            assertEquals(1, preview.getPermissionUpdateCount());
            assertEquals(1, preview.getPermissionDisableCount());
            assertEquals(1, preview.getTenantAdminPermissionAddCount());
            assertEquals(1, preview.getTenantAdminPermissionRemoveCount());

            TenantPermissionSyncResponse result = handler.sync(2L);
            assertEquals(1, result.getPermissionAddCount());
            assertEquals("基础管理平台", targetApplication.getName());
            assertEquals(CommonConst.NO, targetApplication.getUsable());
            assertEquals("系统管理", targetRoot.getPermissionName());
            assertEquals(CommonConst.NO, stale.getUsable());
            assertEquals(3, targetPermissions.size());
            Permission targetChild = targetPermissions.stream()
                    .filter(permission -> Long.valueOf(101L).equals(permission.getSourceId()))
                    .findFirst().orElseThrow();
            assertEquals(targetRoot.getId(), targetChild.getParentId());
            assertEquals(Set.of(targetRoot.getId(), targetChild.getId()), targetGrantIds);
            verify(userRoles).cleanCache(9L);

            TenantPermissionSyncResponse secondPreview = handler.preview(2L);
            assertFalse(secondPreview.isChanged());
        }
        assertEquals(CommonConst.PLATFORM_TENANT_ID, RequestContextHolder.getTenantId());
    }

    private Application application(Long id, Long tenantId, String code, String name) {
        Application application = new Application();
        application.setId(id);
        application.setTenantId(tenantId);
        application.setCode(code);
        application.setName(name);
        application.setDescription("应用描述");
        application.setSortNumber(1);
        application.setOperable(CommonConst.NO);
        application.setUsable(CommonConst.YES);
        application.setVersion(1);
        return application;
    }

    private Permission permission(Long id, Long sourceId, Long parentId, Long appId, String name) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setSourceId(sourceId);
        permission.setParentId(parentId);
        permission.setAppId(appId);
        permission.setTenantId(sourceId == null ? 1L : 2L);
        permission.setPermissionName(name);
        permission.setPermissionType(CommonConst.MENU);
        permission.setDevice("web");
        permission.setSortNumber(1);
        permission.setOperable(CommonConst.NO);
        permission.setUsable(CommonConst.YES);
        permission.setVersion(1);
        return permission;
    }

    private Role role(Long id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }
}
