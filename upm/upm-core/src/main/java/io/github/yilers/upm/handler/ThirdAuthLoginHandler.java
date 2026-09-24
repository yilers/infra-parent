package io.github.yilers.upm.handler;

import cn.hutool.v7.core.text.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.entity.ThirdAuthConfig;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserThird;
import io.github.yilers.upm.request.ThirdAuthLoginAuthorizeRequest;
import io.github.yilers.upm.request.ThirdAuthLoginRequest;
import io.github.yilers.upm.response.LoginResponse;
import io.github.yilers.upm.response.ThirdAuthAuthorizeResponse;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.ThirdAuthConfigService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.upm.service.UserThirdService;
import io.github.yilers.upm.thirdauth.ThirdAuthProvider;
import io.github.yilers.upm.thirdauth.ThirdAuthProviderRegistry;
import io.github.yilers.upm.thirdauth.ThirdAuthUserIdentity;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

/**
 * 第三方账号直接登录业务处理器。
 *
 * <p>只允许已经绑定UPM账号的第三方身份登录，不自动创建用户。租户、平台和设备信息保存在
 * 五分钟有效的一次性state中，回调接口不接收客户端提交的租户身份。</p>
 */
@Component
@RequiredArgsConstructor
public class ThirdAuthLoginHandler {

    private static final long STATE_TIMEOUT_MINUTES = 5;
    private static final String LOGIN_STATE_PREFIX = "login_";

    private final TenantService tenantService;
    private final ThirdAuthConfigService thirdAuthConfigService;
    private final UserThirdService userThirdService;
    private final UserService userService;
    private final ThirdAuthProviderRegistry providerRegistry;
    private final AuthHandler authHandler;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    /**
     * 生成第三方平台登录授权地址。
     */
    public ThirdAuthAuthorizeResponse authorize(ThirdAuthLoginAuthorizeRequest request) {
        Tenant tenant = tenantService.findByCode(request.getTenantCode());
        if (tenant == null || !CommonConst.YES.equals(tenant.getUsable())) {
            throw new CommonException("租户不存在或已停用");
        }
        ThirdAuthProvider provider = providerRegistry.get(request.getPlatform());
        Long previousTenantId = RequestContextHolder.getTenantId();
        try {
            RequestContextHolder.setTenantId(tenant.getId());
            ThirdAuthConfig config = findAvailableConfig(request.getPlatform());
            String state = LOGIN_STATE_PREFIX + UUID.randomUUID().toString().replace("-", "");
            ThirdAuthLoginState stateValue = new ThirdAuthLoginState(
                    tenant.getId(), request.getPlatform(), request.getDevice());
            stateBucket(state).set(writeState(stateValue), Duration.ofMinutes(STATE_TIMEOUT_MINUTES));
            return new ThirdAuthAuthorizeResponse(provider.buildAuthorizeUrl(config, state));
        } finally {
            RequestContextHolder.setTenantId(previousTenantId);
        }
    }

    /**
     * 使用第三方授权码完成UPM登录。
     */
    public LoginResponse login(ThirdAuthLoginRequest request) {
        if (!request.getState().startsWith(LOGIN_STATE_PREFIX)) {
            throw new CommonException("第三方登录状态无效，请重新发起登录");
        }
        ThirdAuthLoginState state = consumeState(request.getState());
        Long previousTenantId = RequestContextHolder.getTenantId();
        try {
            RequestContextHolder.setTenantId(state.tenantId());
            ThirdAuthConfig config = findAvailableConfig(state.platform());
            ThirdAuthUserIdentity identity = providerRegistry.get(state.platform())
                    .exchangeUserIdentity(config, request.getAuthCode());
            UserThird binding = findBinding(identity, state.platform());
            if (binding == null) {
                throw new CommonException("该第三方账号尚未绑定UPM账号，请先使用账号密码登录并完成绑定");
            }
            User user = userService.findById(binding.getUserId());
            if (user == null || !state.tenantId().equals(user.getTenantId())) {
                throw new CommonException("绑定的UPM账号不存在或租户不匹配");
            }
            return authHandler.login(user, state.device());
        } finally {
            RequestContextHolder.setTenantId(previousTenantId);
        }
    }

    private ThirdAuthConfig findAvailableConfig(String platform) {
        ThirdAuthConfig config = thirdAuthConfigService.findByPlatform(platform);
        if (config == null || !CommonConst.YES.equals(config.getUsable())) {
            throw new CommonException("当前租户尚未启用该第三方认证平台");
        }
        if (StrUtil.isBlank(config.getClientId()) || StrUtil.isBlank(config.getClientSecret())
                || StrUtil.isBlank(config.getRedirectUri())) {
            throw new CommonException("第三方认证平台配置不完整");
        }
        return config;
    }

    private UserThird findBinding(ThirdAuthUserIdentity identity, String platform) {
        UserThird binding = userThirdService.findByOpenIdAndPlatform(identity.openId(), platform);
        if (binding == null && StrUtil.isNotBlank(identity.unionId())) {
            binding = userThirdService.findByUnionIdAndPlatform(identity.unionId(), platform);
        }
        return binding;
    }

    private String writeState(ThirdAuthLoginState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JacksonException exception) {
            throw new CommonException("生成第三方登录状态失败");
        }
    }

    private ThirdAuthLoginState consumeState(String state) {
        String value = stateBucket(state).getAndDelete();
        if (StrUtil.isBlank(value)) {
            throw new CommonException("第三方登录状态已过期，请重新发起登录");
        }
        try {
            return objectMapper.readValue(value, ThirdAuthLoginState.class);
        } catch (JacksonException exception) {
            throw new CommonException("第三方登录状态无效，请重新发起登录");
        }
    }

    private RBucket<String> stateBucket(String state) {
        return redissonClient.getBucket(CommonConst.THIRD_AUTH_LOGIN_STATE_CACHE_NAME + state);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ThirdAuthLoginState(Long tenantId, String platform, String device) {
    }
}
