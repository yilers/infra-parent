package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.request.TenantRequest;
import io.github.yilers.upm.mapper.TenantMapper;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.controller.TenantController;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import io.github.yilers.upm.service.TenantServiceImpl;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.TenantLoginPolicyService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantAccessHandlerTest {
    private final TenantAccessHandler access = new TenantAccessHandler();

    @AfterEach
    void clear() {
        RequestContextHolder.clear();
    }

    @Test
    void onlyPlatformTenantAdministratorMayCrossTenantsOrCreateTenants() {
        try (var stp = mockStatic(StpUtil.class)) {
            RequestContextHolder.setTenantId(2L);
            assertDoesNotThrow(() -> access.checkTenant(2L));
            assertThrows(CommonException.class, () -> access.checkTenant(3L));
            assertThrows(CommonException.class, access::checkPlatformAdministrator);
            stp.when(() -> StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(true);
            assertThrows(CommonException.class, () -> access.checkTenant(3L));
            RequestContextHolder.setTenantId(CommonConst.PLATFORM_TENANT_ID);
            assertDoesNotThrow(() -> access.checkTenant(3L));
            assertDoesNotThrow(access::checkPlatformAdministrator);
            RequestContextHolder.clear();
            assertThrows(CommonException.class, () -> access.checkTenant(3L));
        }
    }

    @Test
    void foreignTenantUpdateIsRejectedBeforeQueryOrWrite() {
        TenantMapper mapper = mock(TenantMapper.class);
        TenantServiceImpl service = new TenantServiceImpl(mapper, access);
        TenantRequest request = new TenantRequest();
        request.setId(3L);
        RequestContextHolder.setTenantId(2L);
        try (var stp = mockStatic(StpUtil.class)) {
            assertThrows(CommonException.class, () -> service.updateTenant(request));
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void foreignLoginPolicyReadWriteAndDeleteAreRejectedBeforeDatabaseAccess() {
        TenantService tenants = mock(TenantService.class);
        TenantLoginPolicyService policies = mock(TenantLoginPolicyService.class);
        TenantLoginPolicyHandler handler = new TenantLoginPolicyHandler(policies, tenants, access);
        RequestContextHolder.setTenantId(2L);
        try (var stp = mockStatic(StpUtil.class)) {
            assertThrows(CommonException.class, () -> handler.find(3L));
            assertThrows(CommonException.class, () -> handler.save(3L, null));
            assertThrows(CommonException.class, () -> handler.delete(3L));
        }
        verifyNoInteractions(tenants, policies);
    }

    @Test
    void controllerFiltersListToOwnTenantAndRejectsForeignDetails() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "tenant-test"), Tenant.class);
        TenantService tenants = mock(TenantService.class);
        TenantController controller = new TenantController(tenants, mock(CommonHandler.class), mock(TenantHandler.class),
                mock(TenantPermissionSyncHandler.class), mock(TenantLoginPolicyHandler.class), access);
        RequestContextHolder.setTenantId(2L);
        try (var stp = mockStatic(StpUtil.class)) {
            controller.findAll();
            assertThrows(CommonException.class, () -> controller.findById(3L));
            RequestContextHolder.setTenantId(CommonConst.PLATFORM_TENANT_ID);
            stp.when(() -> StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(true);
            controller.findAll();
        }
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<Tenant>> filters = ArgumentCaptor.forClass(Wrapper.class);
        verify(tenants, times(2)).list(filters.capture());
        var ownFilter = (com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Tenant>) filters.getAllValues().getFirst();
        assertTrue(ownFilter.getSqlSegment().contains("id"));
        assertTrue(ownFilter.getParamNameValuePairs().containsValue(2L));
        assertTrue(filters.getAllValues().get(1).getSqlSegment().isEmpty());
        verify(tenants, never()).getById(3L);
    }
}
