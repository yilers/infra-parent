package io.github.yilers.web.filter;

import io.github.yilers.web.context.RequestContextHolder;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 请求链路关联与上下文清理过滤器。
 *
 * <p>本过滤器在Spring Boot请求观测过滤器建立链路后读取当前Trace ID，
 * 不自行创建Span或生成标识。用户和租户仍由具体认证模块初始化，
 * 请求结束后统一清理线程变量，避免线程复用时发生上下文串用。</p>
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class RequestContextCleanupFilter implements Filter {

    /** 宿主未配置Tracer时仍可运行，链路标识保持为空。 */
    private final ObjectProvider<Tracer> tracerProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            Tracer tracer = tracerProvider.getIfAvailable();
            Span span = tracer != null ? tracer.currentSpan() : null;
            RequestContextHolder.setTraceId(span != null ? span.context().traceId() : null);
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
        }
    }
}
