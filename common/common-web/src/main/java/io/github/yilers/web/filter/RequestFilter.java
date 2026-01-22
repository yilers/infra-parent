package io.github.yilers.web.filter;


import cn.hutool.v7.core.text.StrUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.core.enums.DeviceTypeEnum;
import io.github.yilers.web.context.RequestContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(value = 100)
public class RequestFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        try {
            // 租户信息
            String tenantId = request.getHeader(CommonConst.HEADER_TENANT_ID);
            if (StrUtil.isNotBlank(tenantId)) {
                RequestContextHolder.setTenantId(Long.parseLong(tenantId));
            } else {
                RequestContextHolder.setTenantId(Long.parseLong("1"));
            }

            String device = request.getHeader(CommonConst.HEADER_DEVICE);
            if (StrUtil.isNotBlank(device)) {
                RequestContextHolder.setDeviceType(device);
            } else {
                RequestContextHolder.setDeviceType(DeviceTypeEnum.WEB.getCode());
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            RequestContextHolder.clear();
        }

    }
}
