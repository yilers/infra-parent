package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.text.StrUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.ThirdAuthConfig;
import io.github.yilers.upm.entity.UserThird;
import io.github.yilers.upm.enums.ThirdAuthPlatformEnum;
import io.github.yilers.upm.request.ThirdAuthBindRequest;
import io.github.yilers.upm.response.ThirdAuthAuthorizeResponse;
import io.github.yilers.upm.response.ThirdAuthBindingResponse;
import io.github.yilers.upm.service.ThirdAuthConfigService;
import io.github.yilers.upm.service.UserThirdService;
import io.github.yilers.upm.thirdauth.ThirdAuthProvider;
import io.github.yilers.upm.thirdauth.ThirdAuthProviderRegistry;
import io.github.yilers.upm.thirdauth.ThirdAuthUserIdentity;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 当前用户第三方账号绑定业务处理器。
 *
 * <p>绑定流程要求用户已经登录UPM。OAuth2 state在Redis中保存五分钟，记录发起人的用户和租户，
 * 完成绑定时一次性消费，防止授权码被其他登录用户复用。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThirdAuthBindingHandler {

    private static final long STATE_TIMEOUT_MINUTES = 5;

    private final ThirdAuthConfigService thirdAuthConfigService;
    private final UserThirdService userThirdService;
    private final ThirdAuthProviderRegistry providerRegistry;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    /**
     * 查询当前用户在全部支持平台上的绑定状态。
     */
    public List<ThirdAuthBindingResponse> findAll() {
        long userId = StpUtil.getLoginIdAsLong();
        return Arrays.stream(ThirdAuthPlatformEnum.values())
                .map(platform -> toResponse(platform, userThirdService.findByUserIdAndPlatform(userId, platform.getCode())))
                .toList();
    }

    /**
     * 生成第三方平台授权地址并保存一次性state。
     */
    public ThirdAuthAuthorizeResponse authorize(String platform) {
        ThirdAuthConfig config = findAvailableConfig(platform);
        ThirdAuthProvider provider = providerRegistry.get(platform);
        ThirdAuthBindState stateValue = new ThirdAuthBindState(
                StpUtil.getLoginIdAsLong(), currentTenantId(), platform);
        String state = UUID.randomUUID().toString().replace("-", "");
        try {
            stateBucket(state).set(objectMapper.writeValueAsString(stateValue),
                    Duration.ofMinutes(STATE_TIMEOUT_MINUTES));
        } catch (JacksonException exception) {
            throw new CommonException("生成第三方认证状态失败");
        }
        return new ThirdAuthAuthorizeResponse(provider.buildAuthorizeUrl(config, state));
    }

    /**
     * 使用授权码完成当前用户账号绑定。
     */
    @Transactional(rollbackFor = Exception.class)
    public void bind(ThirdAuthBindRequest request) {
        ThirdAuthBindState state = consumeState(request.getState());
        long currentUserId = StpUtil.getLoginIdAsLong();
        long currentTenantId = currentTenantId();
        if (!Long.valueOf(currentUserId).equals(state.userId())
                || !Long.valueOf(currentTenantId).equals(state.tenantId())) {
            throw new CommonException("第三方认证状态与当前登录用户不匹配");
        }

        ThirdAuthConfig config = findAvailableConfig(state.platform());
        ThirdAuthUserIdentity identity = providerRegistry.get(state.platform())
                .exchangeUserIdentity(config, request.getAuthCode());
        checkIdentityOwner(identity, state.platform(), currentUserId);

        UserThird binding = userThirdService.findByUserIdAndPlatform(currentUserId, state.platform());
        if (binding == null) {
            binding = new UserThird();
            binding.setUserId(currentUserId);
            binding.setTenantId(currentTenantId);
            binding.setPlatform(state.platform());
            binding.setDeleted(CommonConst.NO);
        }
        binding.setOpenId(identity.openId());
        binding.setUnionId(identity.unionId());
        binding.setExpand(writeSnapshot(identity));
        try {
            userThirdService.saveOrUpdate(binding);
        } catch (DuplicateKeyException exception) {
            throw new CommonException("该第三方账号已经被其他用户绑定");
        }
    }

    /**
     * 解除当前用户指定平台的绑定。
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbind(String platform) {
        providerRegistry.get(platform);
        long userId = StpUtil.getLoginIdAsLong();
        UserThird binding = userThirdService.findByUserIdAndPlatform(userId, platform);
        if (binding == null) {
            throw new CommonException("当前用户尚未绑定该第三方账号");
        }
        userThirdService.physicallyDelete(currentTenantId(), userId, platform);
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

    private ThirdAuthBindState consumeState(String state) {
        String value = stateBucket(state).getAndDelete();
        if (StrUtil.isBlank(value)) {
            throw new CommonException("第三方认证状态已过期，请重新发起绑定");
        }
        try {
            return objectMapper.readValue(value, ThirdAuthBindState.class);
        } catch (JacksonException exception) {
            throw new CommonException("第三方认证状态无效，请重新发起绑定");
        }
    }

    private void checkIdentityOwner(ThirdAuthUserIdentity identity, String platform, long currentUserId) {
        UserThird openIdOwner = userThirdService.findByOpenIdAndPlatform(identity.openId(), platform);
        if (openIdOwner != null && !Long.valueOf(currentUserId).equals(openIdOwner.getUserId())) {
            throw new CommonException("该第三方账号已经被其他用户绑定");
        }
        if (StrUtil.isNotBlank(identity.unionId())) {
            UserThird unionIdOwner = userThirdService.findByUnionIdAndPlatform(identity.unionId(), platform);
            if (unionIdOwner != null && !Long.valueOf(currentUserId).equals(unionIdOwner.getUserId())) {
                throw new CommonException("该第三方账号已经被其他用户绑定");
            }
        }
    }

    private ThirdAuthBindingResponse toResponse(ThirdAuthPlatformEnum platform, UserThird binding) {
        ThirdAuthBindingResponse response = new ThirdAuthBindingResponse();
        response.setPlatform(platform.getCode());
        response.setPlatformName(platform.getName());
        ThirdAuthConfig config = thirdAuthConfigService.findByPlatform(platform.getCode());
        response.setAvailable(config != null && CommonConst.YES.equals(config.getUsable()));
        response.setBound(binding != null);
        if (binding != null) {
            ThirdAuthUserSnapshot snapshot = readSnapshot(binding.getExpand());
            response.setNickname(snapshot.nickname());
            response.setAvatarUrl(snapshot.avatarUrl());
            response.setBindTime(binding.getCreateTime());
        }
        return response;
    }

    private String writeSnapshot(ThirdAuthUserIdentity identity) {
        try {
            return objectMapper.writeValueAsString(new ThirdAuthUserSnapshot(
                    identity.nickname(), identity.avatarUrl()));
        } catch (JacksonException exception) {
            throw new CommonException("保存第三方用户信息失败");
        }
    }

    private ThirdAuthUserSnapshot readSnapshot(String value) {
        if (StrUtil.isBlank(value)) {
            return new ThirdAuthUserSnapshot(null, null);
        }
        try {
            return objectMapper.readValue(value, ThirdAuthUserSnapshot.class);
        } catch (JacksonException exception) {
            log.warn("第三方用户公开资料解析失败，数据长度: {}", value.length());
            return new ThirdAuthUserSnapshot(null, null);
        }
    }

    private RBucket<String> stateBucket(String state) {
        return redissonClient.getBucket(CommonConst.THIRD_AUTH_BIND_STATE_CACHE_NAME + state);
    }

    private long currentTenantId() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new CommonException("无法获取当前租户信息");
        }
        return tenantId;
    }

    private record ThirdAuthBindState(Long userId, Long tenantId, String platform) {
    }

    private record ThirdAuthUserSnapshot(String nickname, String avatarUrl) {
    }
}
