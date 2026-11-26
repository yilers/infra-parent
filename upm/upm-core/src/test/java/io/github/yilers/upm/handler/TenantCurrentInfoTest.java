package io.github.yilers.upm.handler;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.controller.TenantController;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 区分登录后的展示查询与管理详情，防止为解决初始化报错而放开任意租户详情。 */
class TenantCurrentInfoTest {
    private final TenantService tenants = mock(TenantService.class);
    private final TenantHandler handler = new TenantHandler(tenants, mock(UserService.class), mock(AuthHandler.class));

    @AfterEach
    void clear() {
        RequestContextHolder.clear();
    }

    @Test
    void ordinaryUserOnlyReadsTrustedTenantAndWhitelistedDisplayFields() {
        RequestContextHolder.setTenantId(2L);
        Tenant tenant = tenant();
        tenant.setExpand("{\"name\":\"测试工作空间\",\"logo\":\"/tenant-logo.png\",\"privateConfig\":\"not-exposed\"}");
        when(tenants.getById(2L)).thenReturn(tenant);
        var response = handler.currentInfo();
        assertEquals(2L, response.id());
        assertEquals("测试工作空间", response.title());
        assertEquals("/tenant-logo.png", response.logo());
        var json = JsonMapper.builder().build().valueToTree(response);
        assertFalse(json.has("expand"));
        assertFalse(json.has("version"));
        assertFalse(json.has("operable"));
        assertFalse(json.toString().contains("not-exposed"));
        verify(tenants).getById(2L);
        verifyNoMoreInteractions(tenants);
    }

    @Test
    void missingContextCannotReadEvenDefaultTenant() {
        assertThrows(CommonException.class, handler::currentInfo);
        verifyNoInteractions(tenants);
    }

    @Test
    void disabledOrMissingTenantIsRejectedAndInvalidExpandFallsBack() {
        RequestContextHolder.setTenantId(2L);
        assertThrows(CommonException.class, handler::currentInfo);
        Tenant tenant = tenant();
        tenant.setUsable(CommonConst.NO);
        when(tenants.getById(2L)).thenReturn(tenant);
        assertThrows(CommonException.class, handler::currentInfo);
        tenant.setUsable(CommonConst.YES);
        tenant.setExpand("not-json");
        assertEquals("测试租户", handler.currentInfo().title());
        assertNull(handler.currentInfo().logo());
    }

    @Test
    void displayEndpointRequiresLoginWhileManagementDetailKeepsPermissions() throws Exception {
        var current = TenantController.class.getMethod("current");
        assertNotNull(current.getAnnotation(SaCheckLogin.class));
        assertNull(current.getAnnotation(SaCheckPermission.class));
        var detail = TenantController.class.getMethod("findById", Long.class);
        assertNotNull(detail.getAnnotation(SaCheckPermission.class));
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(2L);
        tenant.setCode("example.com");
        tenant.setName("测试租户");
        tenant.setUsable(CommonConst.YES);
        return tenant;
    }
}
