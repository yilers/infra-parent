package io.github.yilers.upm.handler;

import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.request.TenantRequest;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.context.RequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TenantApplicationCopyTest {
    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void copiesAllApplicationsAndMenusButOnlyExistingAdminGrants() {
        UserService users = mock(UserService.class);
        DeptService departments = mock(DeptService.class);
        PermissionService permissions = mock(PermissionService.class);
        TenantService tenants = mock(TenantService.class);
        RoleService roles = mock(RoleService.class);
        DeviceService devices = mock(DeviceService.class);
        RolePermissionService grants = mock(RolePermissionService.class);
        ApplicationService applications = mock(ApplicationService.class);
        CommonHandler handler = new CommonHandler(users, departments, mock(RoleDeptService.class),
                mock(UserRoleService.class), mock(UserDataScopeService.class), permissions, tenants,
                roles, devices, grants, applications);
        AtomicLong sequence = new AtomicLong(1000);
        when(tenants.findAll()).thenReturn(List.of(new Tenant()));
        when(departments.save(any())).thenAnswer(invocation -> {
            ((Dept) invocation.getArgument(0)).setId(sequence.incrementAndGet());
            return true;
        });
        when(roles.save(any())).thenAnswer(invocation -> {
            ((Role) invocation.getArgument(0)).setId(sequence.incrementAndGet());
            return true;
        });
        when(users.save(any())).thenAnswer(invocation -> {
            ((User) invocation.getArgument(0)).setId(sequence.incrementAndGet());
            return true;
        });
        Application builtIn = application(1L, "infra", 0, 1);
        Application oa = application(2L, "oa", 1, 0);
        when(applications.list()).thenAnswer(invocation -> {
            assertEquals(1L, RequestContextHolder.getTenantId());
            return List.of(builtIn, oa);
        });
        List<Application> copiedApplications = new ArrayList<>();
        when(applications.save(any())).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            application.setId(sequence.incrementAndGet());
            copiedApplications.add(application);
            return true;
        });
        Device web = new Device();
        web.setCode("web");
        Device app = new Device();
        app.setCode("app");
        when(devices.list()).thenReturn(List.of(web, app));
        Role platformRole = new Role();
        platformRole.setId(10L);
        Role tenantRole = new Role();
        tenantRole.setId(20L);
        when(roles.findByRoleCode(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(platformRole);
        when(roles.findByRoleCode(CommonConst.TENANT_ADMIN_ROLE_CODE)).thenReturn(tenantRole);
        Permission root = permission(100L, 0L, 1L, "基础菜单", 0, 1);
        Permission child = permission(101L, 100L, 1L, "子菜单", 1, 1);
        Permission unassigned = permission(200L, 0L, 2L, "未授权停用菜单", 1, 0);
        unassigned.setDevice("app");
        when(permissions.list()).thenReturn(List.of(root, child, unassigned));
        when(grants.findPermissionListByRoleId(10L)).thenReturn(List.of(root, child));
        when(grants.findPermissionListByRoleId(20L)).thenReturn(List.of(root));
        List<Permission> copiedMenus = new ArrayList<>();
        when(permissions.saveBatch(anyCollection())).thenAnswer(invocation -> {
            copiedMenus.addAll(invocation.<Collection<Permission>>getArgument(0));
            return true;
        });
        List<RolePermission> copiedGrants = new ArrayList<>();
        when(grants.saveBatch(anyCollection())).thenAnswer(invocation -> {
            copiedGrants.addAll(invocation.<Collection<RolePermission>>getArgument(0));
            return true;
        });

        RequestContextHolder.setTenantId(88L);
        TenantRequest request = new TenantRequest();
        request.setCode("example.com");
        request.setName("测试租户");
        handler.addTenant(request);

        assertEquals(88L, RequestContextHolder.getTenantId());
        assertEquals(2, copiedApplications.size());
        assertEquals(0, copiedApplications.getFirst().getOperable());
        assertEquals(0, copiedApplications.get(1).getUsable());
        assertEquals(3, copiedMenus.size());
        Permission copiedRoot = copiedMenus.stream().filter(p -> "基础菜单".equals(p.getPermissionName())).findFirst().orElseThrow();
        Permission copiedChild = copiedMenus.stream().filter(p -> "子菜单".equals(p.getPermissionName())).findFirst().orElseThrow();
        Permission copiedUnassigned = copiedMenus.stream().filter(p -> "未授权停用菜单".equals(p.getPermissionName())).findFirst().orElseThrow();
        assertEquals(copiedRoot.getId(), copiedChild.getParentId());
        assertEquals(copiedApplications.getFirst().getId(), copiedChild.getAppId());
        assertEquals(copiedApplications.get(1).getId(), copiedUnassigned.getAppId());
        assertEquals(0, copiedRoot.getOperable());
        assertEquals(0, copiedUnassigned.getUsable());
        assertEquals("app", copiedUnassigned.getDevice());
        assertTrue(copiedMenus.stream().allMatch(p -> p.getTenantId().equals(20L)));
        assertEquals(3, copiedGrants.size());
        assertTrue(copiedGrants.stream().noneMatch(g -> g.getPermissionId().equals(copiedUnassigned.getId())));
        assertEquals(100L, root.getId());
        assertEquals(100L, child.getParentId());
        verify(devices, times(2)).save(any(Device.class));
        verify(users, times(2)).save(any(User.class));
    }

    private Application application(Long id, String code, int operable, int usable) {
        Application application = new Application();
        application.setId(id);
        application.setCode(code);
        application.setOperable(operable);
        application.setUsable(usable);
        application.setTenantId(1L);
        return application;
    }

    private Permission permission(Long id, Long parentId, Long appId, String name, int operable, int usable) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setParentId(parentId);
        permission.setAppId(appId);
        permission.setPermissionName(name);
        permission.setOperable(operable);
        permission.setUsable(usable);
        permission.setDevice("web");
        permission.setTenantId(1L);
        return permission;
    }
}
