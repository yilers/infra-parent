package io.github.yilers.web.permission;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataPermissionResourceResolverTest {

    @Test
    void resolvesMatchedRouteTemplate() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/leave/123");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/leave/{id}");

        assertEquals("/leave/{id}", DataPermissionResourceResolver.resolve(request));
    }

    @Test
    void fallsBackToServletPathWithoutMatchedTemplate() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/leave/123");

        assertEquals("/leave/123", DataPermissionResourceResolver.resolve(request));
    }
}
