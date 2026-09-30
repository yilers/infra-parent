package io.github.yilers.upm.handler;

import io.github.yilers.upm.entity.Application;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.request.ApplicationRequest;
import io.github.yilers.upm.response.ApplicationSecretResponse;
import io.github.yilers.upm.service.ApplicationService;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.sso.SsoSecretCipher;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationHandlerTest {
    private final ApplicationService service = mock(ApplicationService.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final SsoSecretCipher secretCipher = mock(SsoSecretCipher.class);
    private final ApplicationHandler handler = new ApplicationHandler(service, tenantService, secretCipher);

    @Test
    void createsOnlyAnEmptyEnabledOperableApplication() {
        ApplicationRequest request = new ApplicationRequest();
        request.setId(999L);
        request.setName("OA");
        request.setCode("oa");
        handler.save(request);
        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(service).save(captor.capture());
        Application application = captor.getValue();
        assertNull(application.getId());
        assertEquals(1, application.getOperable());
        assertEquals(1, application.getUsable());
        assertEquals(0, application.getDeleted());
        assertEquals(0, application.getSsoEnabled());
        verify(service).findByCode("oa");
        verifyNoMoreInteractions(service);
    }

    @Test
    void builtInApplicationCannotBeEditedOrDisabled() {
        Application application = new Application();
        application.setId(1L);
        application.setOperable(0);
        when(service.getById(1L)).thenReturn(application);
        ApplicationRequest request = new ApplicationRequest();
        request.setId(1L);
        assertThrows(CommonException.class, () -> handler.update(request));
        assertThrows(CommonException.class, () -> handler.usable(1L));
        verify(service, never()).updateById(any(Application.class));
    }

    @Test
    void rejectsChangingCodeAndDuplicateCode() {
        Application application = new Application();
        application.setId(2L);
        application.setOperable(1);
        application.setCode("oa");
        when(service.getById(2L)).thenReturn(application);
        when(service.findByCode("oa")).thenReturn(application);
        ApplicationRequest request = new ApplicationRequest();
        request.setId(2L);
        request.setCode("crm");
        assertThrows(CommonException.class, () -> handler.update(request));
        request.setCode("oa");
        assertThrows(CommonException.class, () -> handler.save(request));
    }

    @Test
    void returnsNewVersionAfterResettingSecret() {
        Application application = new Application();
        application.setId(2L);
        application.setTenantId(1L);
        application.setOperable(1);
        application.setCode("oa");
        application.setVersion(2);
        Tenant tenant = new Tenant();
        tenant.setCode("yilers.com");
        when(service.getById(2L)).thenReturn(application);
        when(service.updateById(application)).thenAnswer(invocation -> {
            application.setVersion(application.getVersion() + 1);
            return true;
        });
        when(tenantService.getById(1L)).thenReturn(tenant);
        when(secretCipher.encrypt(anyString())).thenReturn("encrypted-secret");

        ApplicationSecretResponse response = handler.resetSecret(2L, 2);

        assertEquals(3, response.version());
        assertEquals("yilers.com:oa", response.clientId());
        assertNotNull(response.secret());
        assertNotNull(response.secretUpdateTime());
        assertEquals("encrypted-secret", application.getSsoSecret());
    }
}
