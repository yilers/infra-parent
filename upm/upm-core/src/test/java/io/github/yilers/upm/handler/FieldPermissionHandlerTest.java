package io.github.yilers.upm.handler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.github.yilers.api.field.*;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.request.RoleFieldRequest;
import io.github.yilers.upm.response.UserManagementResponse;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 验证角色字段权限并集、租户边界及不能越过当前人的授权上限。 */
class FieldPermissionHandlerTest {
    private final FieldService fields = mock(FieldService.class);
    private final RoleFieldService grants = mock(RoleFieldService.class);
    private final RoleService roles = mock(RoleService.class);
    private final UserRoleService users = mock(UserRoleService.class);
    private final TenantService tenants = mock(TenantService.class);
    private final FieldPermissionHandler handler = new FieldPermissionHandler(fields, grants, roles, users, tenants,
            List.of(() -> FieldDefinition.fromResponse(UserManagementResponse.class)));

    @BeforeEach
    void setup() {
        RequestContextHolder.setTenantId(1L);
        RequestContextHolder.setUserId(100L);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "field-test"), Role.class);
        UserRole relation = new UserRole();
        relation.setRoleId(10L);
        when(users.list(org.mockito.ArgumentMatchers.<Wrapper<UserRole>>any())).thenReturn(List.of(relation));
        when(roles.listByIds(anyCollection())).thenReturn(List.of(role(10L, "employee")));
        when(roles.getById(20L)).thenReturn(role(20L, "target"));
        Field field = new Field();
        field.setId(200L);
        field.setTenantId(1L);
        field.setResourceCode("system:user");
        field.setFieldCode("phone");
        field.setMaskType("PHONE");
        field.setUsable(CommonConst.YES);
        field.setWriteSupported(true);
        when(fields.list()).thenReturn(List.of(field));
        when(roles.update(org.mockito.ArgumentMatchers.<Wrapper<Role>>any())).thenReturn(true);
    }

    @AfterEach
    void clear() {
        RequestContextHolder.clear();
    }

    @Test
    void synchronizesTenantCatalogWithoutAnyApplicationConfiguration() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setUsable(CommonConst.YES);
        when(tenants.getOne(org.mockito.ArgumentMatchers.<Wrapper<Tenant>>any())).thenReturn(tenant);
        when(fields.saveOrUpdate(any(Field.class))).thenReturn(true);
        handler.synchronize();
        var captor = org.mockito.ArgumentCaptor.forClass(Field.class);
        verify(fields, times(2)).saveOrUpdate(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(field -> field.getTenantId().equals(1L)));
        assertTrue(captor.getAllValues().stream().allMatch(field -> "system:user".equals(field.getResourceCode())));
    }

    @Test
    void combinesRoleGrants() {
        var masked = grant(FieldViewMode.MASKED, false);
        var full = grant(FieldViewMode.FULL, true);
        full.setRoleId(11L);
        UserRole first = new UserRole();
        first.setRoleId(10L);
        UserRole second = new UserRole();
        second.setRoleId(11L);
        when(users.list(org.mockito.ArgumentMatchers.<Wrapper<UserRole>>any())).thenReturn(List.of(first, second));
        when(roles.listByIds(anyCollection())).thenReturn(List.of(role(10L, "employee"), role(11L, "manager")));
        when(grants.list(org.mockito.ArgumentMatchers.<Wrapper<RoleField>>any())).thenReturn(List.of(masked, full));
        assertEquals(FieldAccess.FULL, handler.currentAccess().get("system:user:phone"));
    }

    @Test
    void hidingFieldPhysicallyRemovesOnlySubmittedGrant() {
        when(roles.listByIds(anyCollection())).thenReturn(List.of(role(10L, CommonConst.TENANT_ADMIN_ROLE_CODE)));
        handler.save(request(FieldViewMode.HIDDEN, false));
        verify(grants).remove(org.mockito.ArgumentMatchers.<Wrapper<RoleField>>any());
        verify(grants, never()).save(any(RoleField.class));
    }

    @Test
    void grantingFieldRemovesOldAssociationThenCreatesNewOne() {
        when(roles.listByIds(anyCollection())).thenReturn(List.of(role(10L, CommonConst.TENANT_ADMIN_ROLE_CODE)));
        when(grants.save(any(RoleField.class))).thenReturn(true);
        handler.save(request(FieldViewMode.MASKED, false));
        var order = inOrder(grants);
        order.verify(grants).remove(org.mockito.ArgumentMatchers.<Wrapper<RoleField>>any());
        var captor = org.mockito.ArgumentCaptor.forClass(RoleField.class);
        order.verify(grants).save(captor.capture());
        assertEquals(20L, captor.getValue().getRoleId());
        assertEquals(200L, captor.getValue().getFieldId());
        assertEquals("MASKED", captor.getValue().getViewMode());
    }

    @Test
    void rejectsForeignRoleBeforeWriting() {
        Role foreign = role(20L, "target");
        foreign.setTenantId(2L);
        when(roles.getById(20L)).thenReturn(foreign);
        assertThrows(CommonException.class, () -> handler.save(request(FieldViewMode.FULL, true)));
        verify(roles, never()).update(org.mockito.ArgumentMatchers.<Wrapper<Role>>any());
        verify(grants, never()).save(any(RoleField.class));
    }

    @Test
    void cannotGrantFullVisibilityOrEditWhenOnlyMasked() {
        when(grants.list(org.mockito.ArgumentMatchers.<Wrapper<RoleField>>any()))
                .thenReturn(List.of(grant(FieldViewMode.MASKED, false)));
        assertThrows(CommonException.class, () -> handler.save(request(FieldViewMode.FULL, true)));
        verify(grants, never()).save(any(RoleField.class));
    }

    @Test
    void rejectsStaleVersionAndDoesNotChangeAnyGrant() {
        when(roles.update(org.mockito.ArgumentMatchers.<Wrapper<Role>>any())).thenReturn(false);
        assertThrows(CommonException.class, () -> handler.save(request(FieldViewMode.HIDDEN, false)));
        verify(grants, never()).save(any(RoleField.class));
    }

    @Test
    void builtInAdministratorCanBootstrapWithoutAnyStoredGrant() {
        when(roles.listByIds(anyCollection())).thenReturn(List.of(role(10L, CommonConst.TENANT_ADMIN_ROLE_CODE)));
        assertEquals(FieldAccess.FULL, handler.currentAccess().get("system:user:phone"));
        assertEquals(FieldAccess.FULL, handler.currentAccess().get("system:user:email"));
    }

    private RoleFieldRequest request(FieldViewMode mode, boolean edit) {
        return new RoleFieldRequest(20L, 1, List.of(new RoleFieldRequest.Grant(200L, mode, edit)));
    }

    private RoleField grant(FieldViewMode mode, boolean edit) {
        RoleField grant = new RoleField();
        grant.setFieldId(200L);
        grant.setRoleId(10L);
        grant.setTenantId(1L);
        grant.setViewMode(mode.name());
        grant.setEditable(edit);
        return grant;
    }

    private Role role(Long id, String code) {
        Role role = new Role();
        role.setId(id);
        role.setTenantId(1L);
        role.setUsable(CommonConst.YES);
        role.setOperable(CommonConst.YES);
        role.setVersion(1);
        role.setRoleCode(code);
        return role;
    }
}
