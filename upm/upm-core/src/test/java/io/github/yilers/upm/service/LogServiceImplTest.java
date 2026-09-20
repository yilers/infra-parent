package io.github.yilers.upm.service;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Log;
import io.github.yilers.upm.mapper.LogMapper;
import io.github.yilers.web.context.RequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class LogServiceImplTest {
    private final LogMapper logMapper = mock(LogMapper.class);
    private final LogServiceImpl logService = new LogServiceImpl(logMapper);

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.clear();
    }

    @Test
    void platformAdministratorCanQuerySpecifiedTenant() {
        RequestContextHolder.setTenantId(1L);
        BasePageRequest<Log> request = createRequest(2L);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(true);
            logService.findByPage(request);
        }

        assertEquals(2L, request.getData().getTenantId());
        verify(logMapper).findByPage(any(), any());
    }

    @Test
    void ordinaryUserCanOnlyQueryCurrentTenant() {
        RequestContextHolder.setTenantId(1L);
        BasePageRequest<Log> request = createRequest(2L);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(false);
            logService.findByPage(request);
        }

        assertEquals(1L, request.getData().getTenantId());
        verify(logMapper).findByPage(any(), any());
    }

    @Test
    void platformAdministratorDefaultsToCurrentTenant() {
        RequestContextHolder.setTenantId(1L);
        BasePageRequest<Log> request = createRequest(null);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)).thenReturn(true);
            logService.findByPage(request);
        }

        assertEquals(1L, request.getData().getTenantId());
    }

    private BasePageRequest<Log> createRequest(Long tenantId) {
        BasePageRequest<Log> request = new BasePageRequest<>();
        request.setCurrent(1L);
        request.setSize(10L);
        Log data = new Log();
        data.setTenantId(tenantId);
        request.setData(data);
        return request;
    }
}
