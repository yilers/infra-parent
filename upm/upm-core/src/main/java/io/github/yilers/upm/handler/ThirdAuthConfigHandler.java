package io.github.yilers.upm.handler;

import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.text.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.ThirdAuthConfig;
import io.github.yilers.upm.enums.ThirdAuthPlatformEnum;
import io.github.yilers.upm.request.ThirdAuthConfigRequest;
import io.github.yilers.upm.response.ThirdAuthConfigResponse;
import io.github.yilers.upm.service.ThirdAuthConfigService;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 第三方认证平台配置业务处理器。
 *
 * <p>配置管理和具体平台授权逻辑分离。当前处理器只负责租户配置的校验、保存和启停，
 * 后续钉钉登录通过Provider读取已经启用的配置。</p>
 */
@Component
@RequiredArgsConstructor
public class ThirdAuthConfigHandler {

    private final ThirdAuthConfigService thirdAuthConfigService;

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
            throw new CommonException("该第三方认证平台已经配置");
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
            throw new CommonException("第三方认证平台不可修改");
        }
        fillConfiguration(config, request, false);
        config.setVersion(request.getVersion());
        if (!thirdAuthConfigService.updateById(config)) {
            throw new CommonException("更新失败 数据已经变更");
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
            throw new CommonException("更新失败 数据已经变更");
        }
    }

    private ThirdAuthConfig findOperable(Long id) {
        ThirdAuthConfig config = thirdAuthConfigService.getById(id);
        if (config == null) {
            throw new CommonException("第三方认证配置不存在");
        }
        if (!CommonConst.YES.equals(config.getOperable())) {
            throw new CommonException("内置配置不可操作");
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
        if (!ThirdAuthPlatformEnum.supports(platform)) {
            throw new CommonException("暂不支持该第三方认证平台");
        }
    }

    private void validateCompleteConfiguration(ThirdAuthConfig config) {
        if (StrUtil.isBlank(config.getClientId())) {
            throw new CommonException("请先配置Client ID");
        }
        if (StrUtil.isBlank(config.getClientSecret())) {
            throw new CommonException("请先配置Client Secret");
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
            throw new CommonException("授权回调地址格式不正确");
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
