package io.github.yilers.web.filter;

import io.github.yilers.web.context.RequestContextHolder;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RequestContextCleanupFilterTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void clearsContextAfterRequest() throws Exception {
        RequestContextCleanupFilter filter = filter(null);

        filter.doFilter(null, null, (request, response) -> {
            RequestContextHolder.setUserId(7L);
            RequestContextHolder.setTenantId(20L);
            assertEquals(7L, RequestContextHolder.getUserId());
            assertEquals(20L, RequestContextHolder.getTenantId());
        });

        assertNull(RequestContextHolder.getUserId());
        assertNull(RequestContextHolder.getTenantId());
    }

    @Test
    void associatesCurrentTraceAndPreservesItWhenAuthenticationSetsIdentity() throws Exception {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        String traceId = "803b448a0489f84084905d3093480352";
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(traceId);

        filter(tracer).doFilter(null, null, (request, response) -> {
            RequestContextHolder.setTenantId(20L);
            RequestContextHolder.setUserId(7L);
            assertEquals(traceId, RequestContextHolder.getTraceId());
            assertEquals(traceId, RequestContextHolder.getContext().getTraceId());
        });

        assertNull(RequestContextHolder.getContext());
        // 只读取既有Span，不调用nextSpan等方法创建另一条链路。
        verify(tracer).currentSpan();
        verifyNoMoreInteractions(tracer);
    }

    @Test
    void leavesTraceEmptyWithoutTracerOrCurrentSpan() throws Exception {
        Tracer tracer = mock(Tracer.class);
        for (Tracer candidate : new Tracer[]{null, tracer}) {
            filter(candidate).doFilter(null, null, (request, response) ->
                    assertNull(RequestContextHolder.getTraceId()));
            assertNull(RequestContextHolder.getContext());
        }
    }

    @Test
    void clearsEntireContextWhenBusinessThrows() {
        assertThrows(ServletException.class, () -> filter(null).doFilter(null, null, (request, response) -> {
            RequestContextHolder.setTenantId(20L);
            RequestContextHolder.setTraceId("test-trace");
            throw new ServletException("测试请求异常");
        }));
        assertNull(RequestContextHolder.getContext());
    }

    private RequestContextCleanupFilter filter(Tracer tracer) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        if (tracer != null) {
            factory.registerSingleton("tracer", tracer);
        }
        return new RequestContextCleanupFilter(factory.getBeanProvider(Tracer.class));
    }
}
