package com.github.yilers.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 跨域处理
 * @author hui.zhang
 * @date 2021/1/20 9:39 上午
 */

@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) {}

	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, OPTIONS, DELETE");
		response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
		response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "x-requested-with, Content-Type, Accept, " +
				"Access-Token, Authorization, Shop-Id-List, Tenant-Id, Device, X-Forwarded-For");
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setStatus(HttpStatus.OK.value());
			return;
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}
}
