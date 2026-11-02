package io.github.yilers.shortlink;

import io.github.yilers.shortlink.controller.ShortlinkRedirectController;
import io.github.yilers.shortlink.entity.ShortlinkConfig;
import io.github.yilers.shortlink.entity.ShortlinkDomain;
import io.github.yilers.shortlink.entity.ShortlinkNamespace;
import io.github.yilers.shortlink.mapper.ShortlinkConfigMapper;
import io.github.yilers.shortlink.mapper.ShortlinkDomainMapper;
import io.github.yilers.shortlink.mapper.ShortlinkNamespaceMapper;
import io.github.yilers.shortlink.provider.ShortlinkTenantProvider;
import io.github.yilers.shortlink.support.ShortlinkCache;
import io.github.yilers.web.context.RequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShortlinkRedirectControllerTest {

    private final ShortlinkDomainMapper domains = mock(ShortlinkDomainMapper.class);
    private final ShortlinkNamespaceMapper namespaces = mock(ShortlinkNamespaceMapper.class);
    private final ShortlinkConfigMapper configs = mock(ShortlinkConfigMapper.class);
    private final ShortlinkCache cache = mock(ShortlinkCache.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ShortlinkTenantProvider> tenants = mock(ObjectProvider.class);
    private final ShortlinkTenantProvider tenant = mock(ShortlinkTenantProvider.class);
    private final ShortlinkRedirectController controller = new ShortlinkRedirectController(domains, namespaces, configs, cache, tenants);

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void unavailableDomainAndExpiredLinkReturnSame404Page() throws Exception {
        MockHttpServletResponse missingDomain = redirect();
        configureAvailableNamespace();
        when(cache.find(3L, "V2Vhp9")).thenReturn(new ShortlinkCache.Target("https://example.com/private",
                LocalDateTime.now().minusMinutes(1), 1));
        MockHttpServletResponse expired = redirect();

        assertEquals(404, missingDomain.getStatus());
        assertEquals(404, expired.getStatus());
        assertEquals("no-store", expired.getHeader("Cache-Control"));
        assertTrue(expired.getContentType().startsWith("text/html"));
        assertEquals("UTF-8", expired.getCharacterEncoding());
        assertTrue(expired.getContentAsString().contains("哎呀，链接走丢了"));
        assertFalse(expired.getContentAsString().contains("example.com/private"));
        assertEquals(missingDomain.getContentAsString(), expired.getContentAsString());
    }

    @Test
    void validLinkStillRedirectsToTarget() throws Exception {
        configureAvailableNamespace();
        when(cache.find(3L, "V2Vhp9")).thenReturn(new ShortlinkCache.Target("https://example.com/target", null, 1));
        MockHttpServletResponse response = redirect();
        assertEquals(302, response.getStatus());
        assertEquals("https://example.com/target", response.getHeader("Location"));
        assertEquals("", response.getContentAsString());
    }

    private void configureAvailableNamespace() {
        ShortlinkDomain domain = new ShortlinkDomain();
        domain.setId(2L);
        domain.setTenantId(1L);
        when(domains.findForRedirect("localhost")).thenReturn(domain);
        when(tenants.getIfAvailable()).thenReturn(tenant);
        when(tenant.isUsable(1L)).thenReturn(true);
        ShortlinkNamespace namespace = new ShortlinkNamespace();
        namespace.setId(3L);
        namespace.setConfigId(4L);
        namespace.setCodeLength(6);
        when(namespaces.selectOne(any())).thenReturn(namespace);
        ShortlinkConfig config = new ShortlinkConfig();
        config.setUsable(1);
        when(configs.selectById(4L)).thenReturn(config);
    }

    private MockHttpServletResponse redirect() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.redirect("a", "V2Vhp9", request, response);
        return response;
    }
}
