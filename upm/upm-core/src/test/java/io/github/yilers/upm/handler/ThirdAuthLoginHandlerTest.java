package io.github.yilers.upm.handler;

import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.entity.ThirdAuthConfig;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserThird;
import io.github.yilers.upm.request.ThirdAuthLoginAuthorizeRequest;
import io.github.yilers.upm.request.ThirdAuthLoginRequest;
import io.github.yilers.upm.response.LoginResponse;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.ThirdAuthConfigService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.upm.service.UserThirdService;
import io.github.yilers.upm.thirdauth.ThirdAuthProvider;
import io.github.yilers.upm.thirdauth.ThirdAuthProviderRegistry;
import io.github.yilers.upm.thirdauth.ThirdAuthUserIdentity;
import io.github.yilers.web.context.RequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThirdAuthLoginHandlerTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final ThirdAuthConfigService configService = mock(ThirdAuthConfigService.class);
    private final UserThirdService userThirdService = mock(UserThirdService.class);
    private final UserService userService = mock(UserService.class);
    private final ThirdAuthProviderRegistry providerRegistry = mock(ThirdAuthProviderRegistry.class);
    private final AuthHandler authHandler = mock(AuthHandler.class);
    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RBucket<String> stateBucket = mock(RBucket.class);
    private final ThirdAuthProvider provider = mock(ThirdAuthProvider.class);
    private final ThirdAuthLoginHandler handler = new ThirdAuthLoginHandler(
            tenantService, configService, userThirdService, userService, providerRegistry,
            authHandler, redissonClient, JsonMapper.builder().build());

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void createsTenantScopedOneTimeLoginState() {
        Tenant tenant = tenant();
        ThirdAuthConfig config = config();
        when(tenantService.findByCode("yilers.com")).thenReturn(tenant);
        when(configService.findByPlatform("dingTalk")).thenReturn(config);
        when(providerRegistry.get("dingTalk")).thenReturn(provider);
        when(redissonClient.<String>getBucket(anyString())).thenReturn(stateBucket);
        when(provider.buildAuthorizeUrl(any(), anyString())).thenAnswer(invocation -> {
            assertEquals(1L, RequestContextHolder.getTenantId());
            assertTrue(invocation.<String>getArgument(1).startsWith("login_"));
            return "https://login.dingtalk.com/oauth2/auth";
        });
        RequestContextHolder.setTenantId(99L);
        ThirdAuthLoginAuthorizeRequest request = new ThirdAuthLoginAuthorizeRequest();
        request.setTenantCode("yilers.com");
        request.setPlatform("dingTalk");
        request.setDevice("web");

        assertEquals("https://login.dingtalk.com/oauth2/auth", handler.authorize(request).authorizeUrl());
        assertEquals(99L, RequestContextHolder.getTenantId());
        verify(stateBucket).set(anyString(), any(Duration.class));
    }

    @Test
    void logsInOnlyTheUserBoundInsideStateTenant() {
        String state = "login_test_state";
        when(redissonClient.<String>getBucket(anyString())).thenReturn(stateBucket);
        when(stateBucket.getAndDelete())
                .thenReturn("{\"tenantId\":1,\"platform\":\"dingTalk\",\"device\":\"web\"}");
        when(configService.findByPlatform("dingTalk")).thenReturn(config());
        when(providerRegistry.get("dingTalk")).thenReturn(provider);
        when(provider.exchangeUserIdentity(any(), anyString()))
                .thenReturn(new ThirdAuthUserIdentity("open-id", "union-id", "测试用户", null));
        UserThird binding = new UserThird();
        binding.setUserId(10L);
        when(userThirdService.findByOpenIdAndPlatform("open-id", "dingTalk")).thenReturn(binding);
        User user = new User();
        user.setId(10L);
        user.setTenantId(1L);
        when(userService.findById(10L)).thenReturn(user);
        LoginResponse expected = new LoginResponse();
        expected.setTokenValue("upm-token");
        when(authHandler.login(user, "web")).thenReturn(expected);
        RequestContextHolder.setTenantId(99L);
        ThirdAuthLoginRequest request = new ThirdAuthLoginRequest();
        request.setAuthCode("ding-code");
        request.setState(state);

        assertEquals("upm-token", handler.login(request).getTokenValue());
        assertEquals(99L, RequestContextHolder.getTenantId());
        verify(stateBucket).getAndDelete();
        verify(authHandler).login(user, "web");
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setCode("yilers.com");
        tenant.setUsable(1);
        return tenant;
    }

    private ThirdAuthConfig config() {
        ThirdAuthConfig config = new ThirdAuthConfig();
        config.setClientId("client-id");
        config.setClientSecret("client-secret");
        config.setRedirectUri("https://upm.example.com/third-auth-callback.html");
        config.setUsable(1);
        return config;
    }
}
