package io.github.yilers.web.filter;

import io.github.yilers.web.context.RequestContextHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 请求上下文清理过滤器。
 *
 * <p>上下文数据由具体的认证模块负责初始化，本过滤器只保证请求结束后清理线程变量，
 * 避免线程复用时发生上下文串用。</p>
 */
@Component
@Order(100)
public class RequestContextCleanupFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
        }
    }
}
