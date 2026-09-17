package io.github.yilers.web.filter;

import io.github.yilers.web.context.RequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestContextFilterTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void clearsContextAfterRequest() throws Exception {
        RequestContextFilter filter = new RequestContextFilter();

        filter.doFilter(null, null, (request, response) -> {
            RequestContextHolder.setUserId(7L);
            RequestContextHolder.setTenantId(20L);
            assertEquals(7L, RequestContextHolder.getUserId());
            assertEquals(20L, RequestContextHolder.getTenantId());
        });

        assertNull(RequestContextHolder.getUserId());
        assertNull(RequestContextHolder.getTenantId());
    }
}
