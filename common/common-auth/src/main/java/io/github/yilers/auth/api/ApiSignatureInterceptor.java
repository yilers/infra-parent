package io.github.yilers.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 机器接口认证。先验签再以Redis SET NX原子占用nonce，避免多节点并发重放。
 * Redis或凭证提供者不可用时拒绝调用，不降级放行；与可淘汰的跳转缓存区别对待。
 */
@Component
public class ApiSignatureInterceptor implements HandlerInterceptor {
    private static final String NONCE_PREFIX = "infra:api:nonce:";
    private static final String RATE_PREFIX = "infra:api:rate:";
    private static final DefaultRedisScript<Long> RATE_SCRIPT = new DefaultRedisScript<>(
            "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60) end; return n",
            Long.class);
    private final ObjectProvider<ApiCredentialProvider> credentials;
    private final ObjectProvider<StringRedisTemplate> redis;

    public ApiSignatureInterceptor(ObjectProvider<ApiCredentialProvider> credentials,
                                   ObjectProvider<StringRedisTemplate> redis) {
        this.credentials = credentials;
        this.redis = redis;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!request.getServletPath().startsWith(ApiSignProtocol.PREFIX)) {
            return true;
        }
        // 路径本身不代表放行：未声明能力的机器接口一律拒绝。
        if (!(handler instanceof HandlerMethod method)
                || method.getMethodAnnotation(ApiPermission.class) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "接口未声明系统调用权限");
            return false;
        }
        ApiPermission permission = method.getMethodAnnotation(ApiPermission.class);
        try {
            if (!"POST".equals(request.getMethod()) || request.getQueryString() != null
                    || request.getContentType() == null
                    || !request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "签名入口仅支持无查询串的POST JSON请求");
                return false;
            }
            String key = required(request, ApiSignProtocol.ACCESS_KEY, "[A-Za-z0-9_-]{16,64}");
            String timestamp = required(request, ApiSignProtocol.TIMESTAMP, "[0-9]{13}");
            String nonce = required(request, ApiSignProtocol.NONCE, "[A-Za-z0-9_-]{16,64}");
            String signature = required(request, ApiSignProtocol.SIGNATURE, "[0-9a-f]{64}");
            byte[] body = (byte[]) request.getAttribute(ApiBodyFilter.BODY_ATTRIBUTE);
            if (body == null) {
                throw new IllegalArgumentException("签名正文过滤器未启用");
            }
            ApiCredentialIdentity identity = credentials.getObject().findByAccessKey(key);
            if (identity == null || identity.tenantId() == null || identity.credentialId() == null) {
                throw new IllegalArgumentException("调用凭证无效");
            }
            var template = ApiSignProtocol.template(identity.secret());
            template.checkTimestamp(Long.parseLong(timestamp));
            Map<String, String> values = ApiSignProtocol.parameters(key, request.getMethod(),
                    request.getRequestURI(), timestamp, nonce, body);
            String expected = template.createSign(values);
            if (!java.security.MessageDigest.isEqual(
                    expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    signature.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException("签名不匹配");
            }
            if (!identity.permissions().contains(permission.code())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "调用凭证未获得接口权限");
                return false;
            }
            StringRedisTemplate storage = redis.getObject();
            Boolean accepted = storage.opsForValue().setIfAbsent(
                    NONCE_PREFIX + identity.credentialId() + ":" + nonce, "1",
                    Duration.ofMillis(ApiSignProtocol.WINDOW_MILLIS * 2 + 1000));
            if (!Boolean.TRUE.equals(accepted)) {
                throw new IllegalArgumentException("重复请求");
            }
            Long count = storage.execute(RATE_SCRIPT, List.of(RATE_PREFIX + identity.credentialId()));
            if (count == null || count > identity.requestsPerMinute()) {
                response.sendError(429, "调用频率超过限制");
                return false;
            }
            ApiCallerContext.set(new ApiCallerContext.Caller(identity.credentialId(), identity.tenantId()));
            return true;
        } catch (Exception e) {
            // 不输出签名参数或底层异常，避免把密钥和凭证细节写入日志、响应。
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "系统调用认证失败");
            return false;
        }
    }

    private static String required(HttpServletRequest request, String header, String pattern) {
        String value = request.getHeader(header);
        if (value == null || !value.matches(pattern)) {
            throw new IllegalArgumentException("签名头格式不正确");
        }
        return value;
    }
}
