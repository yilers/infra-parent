package io.github.yilers.upm.handler;

import cn.dev33.satoken.sso.template.SaSsoServerTemplate;
import io.github.yilers.upm.entity.Application;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.response.SsoApplicationResponse;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.upm.sso.SsoClientResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SsoHandlerTest {

    @Test
    void shouldExposeTenantIdInTrustedApplicationContext() {
        SsoClientResolver clientResolver = mock(SsoClientResolver.class);
        SsoHandler handler = new SsoHandler(clientResolver, mock(UserService.class), mock(UserRoleService.class),
                mock(RolePermissionService.class), mock(CommonHandler.class), mock(SaSsoServerTemplate.class));

        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setCode("yilers.com");
        tenant.setName("Yilers");

        Application application = new Application();
        application.setCode("jzy-platform");
        application.setName("JZY Platform");

        SsoClientResolver.Client client = new SsoClientResolver.Client(
                "yilers.com:jzy-platform", tenant, application);
        when(clientResolver.resolve(client.clientId())).thenReturn(client);
        when(clientResolver.requireEnabled(client)).thenReturn(client);

        SsoApplicationResponse response = handler.findApplication(client.clientId());

        assertEquals(1L, response.getTenantId());
        assertEquals("yilers.com", response.getTenantCode());
        assertEquals("jzy-platform", response.getApplicationCode());
    }
}
