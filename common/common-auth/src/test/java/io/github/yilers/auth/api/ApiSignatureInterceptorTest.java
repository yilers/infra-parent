package io.github.yilers.auth.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 校验签名覆盖正文和路径、授权与原子nonce，避免未声明接口被意外开放。 */
class ApiSignatureInterceptorTest {
    private static final String KEY = "abcdefghijklmnop123456789";
    private static final String SECRET = "test-secret";
    private static final String PATH = "/open-api/shortlink/create";
    private final ApiCredentialProvider provider = mock(ApiCredentialProvider.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ApiCredentialProvider> providers = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<StringRedisTemplate> stores = mock(ObjectProvider.class);
    private final ApiSignatureInterceptor interceptor = new ApiSignatureInterceptor(providers, stores);

    static class Controller {
        @ApiPermission(code = "create", name = "创建", group = "测试")
        public void create() {}
        public void unprotected() {}
    }

    @AfterEach void clear() { ApiCallerContext.clear(); }

    @Test
    void validRequestRestoresOnlyCredentialIdentityAndRejectsReplay() throws Exception {
        when(providers.getObject()).thenReturn(provider);
        when(stores.getObject()).thenReturn(redis);
        when(provider.findByAccessKey(KEY)).thenReturn(new ApiCredentialIdentity(7L, 2L, SECRET, Set.of("create"), 60));
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true, false);
        when(redis.execute(any(DefaultRedisScript.class), anyList())).thenReturn(1L);
        var request = signedRequest("{}");
        HandlerMethod handler = new HandlerMethod(new Controller(), Controller.class.getMethod("create"));
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handler));
        assertEquals(new ApiCallerContext.Caller(7L, 2L), ApiCallerContext.current());
        ApiCallerContext.clear();
        var replay = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, replay, handler));
        assertEquals(401, replay.getStatus());
        assertNull(ApiCallerContext.current());
    }

    @Test
    void changingBodyOrPathRejectsBeforeNonceIsStored() throws Exception {
        when(providers.getObject()).thenReturn(provider);
        when(provider.findByAccessKey(KEY)).thenReturn(new ApiCredentialIdentity(7L, 2L, SECRET, Set.of("create"), 60));
        var request = signedRequest("{}");
        request.setAttribute(ApiBodyFilter.BODY_ATTRIBUTE, "{\"tampered\":true}".getBytes(StandardCharsets.UTF_8));
        HandlerMethod handler = new HandlerMethod(new Controller(), Controller.class.getMethod("create"));
        assertFalse(interceptor.preHandle(request, new MockHttpServletResponse(), handler));
        verifyNoInteractions(redis);
        var changedPath = signedRequest("{}");
        changedPath.setRequestURI("/open-api/shortlink/other");
        assertFalse(interceptor.preHandle(changedPath, new MockHttpServletResponse(), handler));
        verifyNoInteractions(redis);
    }

    @Test
    void unsignedUnregisteredOrUnauthorizedCallsAreRejected() throws Exception {
        var unsigned = new MockHttpServletRequest("POST", PATH);
        unsigned.setServletPath(PATH);
        var response = new MockHttpServletResponse();
        HandlerMethod handler = new HandlerMethod(new Controller(), Controller.class.getMethod("unprotected"));
        assertFalse(interceptor.preHandle(unsigned, response, handler));
        assertEquals(403, response.getStatus());
        when(providers.getObject()).thenReturn(provider);
        when(provider.findByAccessKey(KEY)).thenReturn(new ApiCredentialIdentity(7L, 2L, SECRET, Set.of(), 60));
        handler = new HandlerMethod(new Controller(), Controller.class.getMethod("create"));
        response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(signedRequest("{}"), response, handler));
        assertEquals(403, response.getStatus());
        verifyNoInteractions(redis);
    }

    @Test
    void expiredTimestampAndMissingRedisAreRejected() throws Exception {
        when(providers.getObject()).thenReturn(provider);
        when(provider.findByAccessKey(KEY)).thenReturn(new ApiCredentialIdentity(7L, 2L, SECRET, Set.of("create"), 60));
        HandlerMethod handler = new HandlerMethod(new Controller(), Controller.class.getMethod("create"));
        var expired = signedRequest("{}");
        expired.removeHeader(ApiSignProtocol.TIMESTAMP);
        expired.addHeader(ApiSignProtocol.TIMESTAMP, Long.toString(System.currentTimeMillis() - 600_000L));
        var response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(expired, response, handler));
        assertEquals(401, response.getStatus());
        verifyNoInteractions(redis);

        when(stores.getObject()).thenThrow(new IllegalStateException("Redis不可用"));
        response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(signedRequest("{}"), response, handler));
        assertEquals(401, response.getStatus());
        assertNull(ApiCallerContext.current());
    }

    @Test
    void signatureMatchesDocumentedSortedFieldsAndSha256SuffixFormat() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        Map<String, String> fields = ApiSignProtocol.parameters(KEY, "POST", PATH, "1700000000000",
                "nonce-abcdefghijklmnop123456", body);
        String canonical = new java.util.TreeMap<>(fields).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining("&")) + "&key=" + SECRET;
        assertEquals(ApiSignProtocol.sha256(canonical), ApiSignProtocol.template(SECRET).createSign(fields));
    }

    private MockHttpServletRequest signedRequest(String body) {
        String time = Long.toString(System.currentTimeMillis());
        String nonce = "nonce-abcdefghijklmnop123456";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Map<String, String> parameters = ApiSignProtocol.parameters(KEY, "POST", PATH, time, nonce, bytes);
        var request = new MockHttpServletRequest("POST", PATH);
        request.setServletPath(PATH);
        request.setContentType("application/json");
        request.setAttribute(ApiBodyFilter.BODY_ATTRIBUTE, bytes);
        request.addHeader(ApiSignProtocol.ACCESS_KEY, KEY);
        request.addHeader(ApiSignProtocol.TIMESTAMP, time);
        request.addHeader(ApiSignProtocol.NONCE, nonce);
        request.addHeader(ApiSignProtocol.SIGNATURE, ApiSignProtocol.template(SECRET).createSign(parameters));
        return request;
    }
}
