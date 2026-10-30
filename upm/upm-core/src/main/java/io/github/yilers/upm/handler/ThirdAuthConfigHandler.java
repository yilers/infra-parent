package io.github.yilers.upm.handler;

import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.text.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.ThirdAuthConfig;
import io.github.yilers.upm.enums.ThirdAuthPlatformEnum;
import io.github.yilers.upm.request.ThirdAuthConfigRequest;
import io.github.yilers.upm.response.ThirdAuthConfigResponse;
import io.github.yilers.upm.response.ThirdAuthPlatformResponse;
import io.github.yilers.upm.service.ThirdAuthConfigService;
import io.github.yilers.upm.thirdauth.ThirdAuthProviderRegistry;
import io.github.yilers.upm.i18n.UpmAccessMessage;
import io.github.yilers.upm.i18n.UpmManagementMessage;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 第三方认证平台配置业务处理器。
 *
 * <p>配置管理和具体平台授权逻辑分离。当前处理器负责租户配置的校验、保存和启停，
 * 具体授权流程由已注册的平台Provider负责。</p>
 */
@Component
@RequiredArgsConstructor
public class ThirdAuthConfigHandler {

    private final ThirdAuthConfigService thirdAuthConfigService;
    private final ThirdAuthProviderRegistry providerRegistry;

    /**
     * 查询第三方认证平台目录及当前租户的配置状态。
     * 尚未接入的常见平台会展示为暂不支持，不允许被误配置为可登录平台。
     */
    public List<ThirdAuthPlatformResponse> findPlatformCatalog() {
        Map<String, ThirdAuthConfig> configMap = thirdAuthConfigService.list().stream()
                .collect(Collectors.toMap(ThirdAuthConfig::getPlatform, Function.identity(), (left, right) -> left));
        return java.util.Arrays.stream(ThirdAuthPlatformEnum.values())
                .map(platform -> {
                    ThirdAuthConfig config = configMap.get(platform.getCode());
                    boolean implemented = providerRegistry.supports(platform.getCode());
                    boolean configured = config != null;
                    boolean usable = implemented && configured && CommonConst.YES.equals(config.getUsable());
                    return new ThirdAuthPlatformResponse(platform.getCode(), platform.getName(), platform.getIcon(),
                            implemented, configured, usable, config == null ? null : config.getId());
                })
                .toList();
    }

    /**
     * 查询当前租户的全部第三方认证配置。
     */
    public List<ThirdAuthConfigResponse> findAll() {
        return thirdAuthConfigService.list(Wrappers.<ThirdAuthConfig>lambdaQuery()
                        .orderByAsc(ThirdAuthConfig::getPlatform, ThirdAuthConfig::getId)).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 新增第三方认证配置。新配置默认停用，管理员确认配置无误后再单独启用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(ThirdAuthConfigRequest request) {
        validatePlatform(request.getPlatform());
        if (thirdAuthConfigService.findByPlatform(request.getPlatform()) != null) {
            throw new CommonException(UpmAccessMessage.THIRD_CONFIG_EXISTS);
        }
        ThirdAuthConfig config = new ThirdAuthConfig();
        config.setPlatform(request.getPlatform());
        fillConfiguration(config, request, true);
        config.setOperable(CommonConst.YES);
        config.setUsable(CommonConst.NO);
        config.setDeleted(CommonConst.NO);
        config.setVersion(1);
        thirdAuthConfigService.save(config);
    }

    /**
     * 修改第三方认证配置。平台编码创建后不可修改，Client Secret留空时保留原值。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(ThirdAuthConfigRequest request) {
        ThirdAuthConfig config = findOperable(request.getId());
        if (!config.getPlatform().equals(request.getPlatform())) {
            throw new CommonException(UpmAccessMessage.THIRD_PLATFORM_IMMUTABLE);
        }
        fillConfiguration(config, request, false);
        config.setVersion(request.getVersion());
        if (!thirdAuthConfigService.updateById(config)) {
            throw new CommonException(UpmManagementMessage.DATA_UPDATE_CONFLICT);
        }
    }

    /**
     * 启用或停用第三方认证配置。启用前必须保证连接参数完整。
     */
    @Transactional(rollbackFor = Exception.class)
    public void usable(Long id) {
        ThirdAuthConfig config = findOperable(id);
        if (CommonConst.NO.equals(config.getUsable())) {
            validateCompleteConfiguration(config);
            config.setUsable(CommonConst.YES);
        } else {
            config.setUsable(CommonConst.NO);
        }
        if (!thirdAuthConfigService.updateById(config)) {
            throw new CommonException(UpmManagementMessage.DATA_UPDATE_CONFLICT);
        }
    }

    private ThirdAuthConfig findOperable(Long id) {
        ThirdAuthConfig config = thirdAuthConfigService.getById(id);
        if (config == null) {
            throw new CommonException(UpmAccessMessage.THIRD_CONFIG_MISSING);
        }
        if (!CommonConst.YES.equals(config.getOperable())) {
            throw new CommonException(UpmAccessMessage.THIRD_CONFIG_BUILTIN_PROTECTED);
        }
        return config;
    }

    private void fillConfiguration(ThirdAuthConfig config, ThirdAuthConfigRequest request, boolean creating) {
        config.setClientId(StrUtil.trim(request.getClientId()));
        if (creating || StrUtil.isNotBlank(request.getClientSecret())) {
            config.setClientSecret(StrUtil.trim(request.getClientSecret()));
        }
        config.setRedirectUri(normalizeHttpUrl(request.getRedirectUri()));
        config.setScopes(normalizeScopes(request.getScopes()));
        config.setDescription(StrUtil.trim(request.getDescription()));
    }

    private void validatePlatform(String platform) {
        if (!providerRegistry.supports(platform)) {
            throw new CommonException(UpmAccessMessage.THIRD_PLATFORM_UNSUPPORTED);
        }
    }

    private void validateCompleteConfiguration(ThirdAuthConfig config) {
        if (StrUtil.isBlank(config.getClientId())) {
            throw new CommonException(UpmAccessMessage.THIRD_CLIENT_ID_REQUIRED);
        }
        if (StrUtil.isBlank(config.getClientSecret())) {
            throw new CommonException(UpmAccessMessage.THIRD_CLIENT_SECRET_REQUIRED);
        }
        normalizeHttpUrl(config.getRedirectUri());
    }

    private String normalizeHttpUrl(String value) {
        String url = StrUtil.trim(value);
        try {
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || StrUtil.isBlank(uri.getHost())) {
                throw new IllegalArgumentException();
            }
            return url;
        } catch (Exception exception) {
            throw new CommonException(UpmAccessMessage.THIRD_CALLBACK_INVALID);
        }
    }

    private String normalizeScopes(List<String> scopes) {
        if (scopes == null) {
            return null;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String scope : scopes) {
            String value = StrUtil.trim(scope);
            if (StrUtil.isNotBlank(value)) {
                values.add(value);
            }
        }
        return String.join(",", values);
    }

    private ThirdAuthConfigResponse toResponse(ThirdAuthConfig config) {
        ThirdAuthConfigResponse response = BeanUtil.copyProperties(config, ThirdAuthConfigResponse.class);
        response.setPlatformName(ThirdAuthPlatformEnum.getName(config.getPlatform()));
        response.setSecretConfigured(StrUtil.isNotBlank(config.getClientSecret()));
        response.setScopes(StrUtil.isBlank(config.getScopes()) ? List.of() : List.of(config.getScopes().split(",")));
        return response;
    }
}
