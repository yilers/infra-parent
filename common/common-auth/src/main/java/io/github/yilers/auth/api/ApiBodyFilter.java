package io.github.yilers.auth.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** 有界缓存机器接口正文，供验签和Controller各读取一次；无论成功失败均清理机器身份。 */
@Component
@Order(90)
public class ApiBodyFilter extends OncePerRequestFilter {
    public static final String BODY_ATTRIBUTE = ApiBodyFilter.class.getName() + ".body";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(ApiSignProtocol.PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ApiCallerContext.clear();
        try {
            byte[] body = request.getInputStream().readNBytes(ApiSignProtocol.MAX_BODY_BYTES + 1);
            if (body.length > ApiSignProtocol.MAX_BODY_BYTES) {
                response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                return;
            }
            request.setAttribute(BODY_ATTRIBUTE, body);
            chain.doFilter(new HttpServletRequestWrapper(request) {
                @Override
                public ServletInputStream getInputStream() {
                    ByteArrayInputStream input = new ByteArrayInputStream(body);
                    return new ServletInputStream() {
                        @Override public int read() { return input.read(); }
                        @Override public boolean isFinished() { return input.available() == 0; }
                        @Override public boolean isReady() { return true; }
                        @Override public void setReadListener(ReadListener listener) {
                            throw new UnsupportedOperationException("签名JSON入口不支持非阻塞读取");
                        }
                    };
                }
                @Override
                public BufferedReader getReader() {
                    return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
                }
            }, response);
        } finally {
            ApiCallerContext.clear();
        }
    }
}
