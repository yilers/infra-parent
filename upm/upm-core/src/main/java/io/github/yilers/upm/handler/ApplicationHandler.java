package io.github.yilers.upm.handler;

import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.text.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Application;
import io.github.yilers.upm.request.ApplicationRequest;
import io.github.yilers.upm.response.ApplicationResponse;
import io.github.yilers.upm.response.ApplicationSecretResponse;
import io.github.yilers.upm.service.ApplicationService;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.sso.SsoSecretCipher;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApplicationHandler {
    private final SecureRandom secureRandom = new SecureRandom();
    private final ApplicationService applicationService;
    private final TenantService tenantService;
    private final SsoSecretCipher secretCipher;

    public List<ApplicationResponse> findAll() {
        String tenantCode = tenantService.getById(RequestContextHolder.getTenantId()).getCode();
        return applicationService.list(Wrappers.<Application>lambdaQuery()
                        .orderByAsc(Application::getSortNumber, Application::getId)).stream()
                .map(application -> toResponse(application, tenantCode)).toList();
    }

    public Application findById(Long id) {
        Application application = applicationService.getById(id);
        if (application == null) {
            throw new CommonException("应用不存在");
        }
        return application;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(ApplicationRequest dto) {
        if (applicationService.findByCode(dto.getCode()) != null) {
            throw new CommonException("应用编码已存在");
        }
        Application application = new Application();
        application.setName(dto.getName());
        application.setCode(dto.getCode());
        application.setIcon(dto.getIcon());
        application.setLogo(dto.getLogo());
        application.setHomeUrl(dto.getHomeUrl());
        application.setDescription(dto.getDescription());
        application.setSortNumber(dto.getSortNumber());
        application.setUsable(CommonConst.YES);
        application.setOperable(CommonConst.YES);
        application.setSsoEnabled(CommonConst.NO);
        application.setDeleted(CommonConst.NO);
        application.setVersion(1);
        applicationService.save(application);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ApplicationRequest dto) {
        Application application = findOperable(dto.getId());
        if (dto.getCode() != null && !application.getCode().equals(dto.getCode())) {
            throw new CommonException("应用编码不可修改");
        }
        application.setName(dto.getName());
        application.setIcon(dto.getIcon());
        application.setLogo(dto.getLogo());
        application.setHomeUrl(dto.getHomeUrl());
        application.setRedirectUris(normalizeRedirectUris(dto.getRedirectUris()));
        application.setSsoPushUrl(StrUtil.trim(dto.getSsoPushUrl()));
        if (CommonConst.YES.equals(dto.getSsoEnabled())) {
            validateSsoConfiguration(application);
        }
        application.setSsoEnabled(CommonConst.YES.equals(dto.getSsoEnabled()) ? CommonConst.YES : CommonConst.NO);
        application.setDescription(dto.getDescription());
        application.setSortNumber(dto.getSortNumber());
        application.setVersion(dto.getVersion());
        if (!applicationService.updateById(application)) {
            throw new CommonException("更新失败 数据已经变更");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApplicationSecretResponse resetSecret(Long id) {
        Application application = findOperable(id);
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        application.setSsoSecret(secretCipher.encrypt(secret));
        application.setSecretUpdateTime(LocalDateTime.now());
        if (!applicationService.updateById(application)) {
            throw new CommonException("更新失败 数据已经变更");
        }
        String tenantCode = tenantService.getById(application.getTenantId()).getCode();
        return new ApplicationSecretResponse(clientId(tenantCode, application.getCode()), secret);
    }

    @Transactional(rollbackFor = Exception.class)
    public void usable(Long id) {
        Application application = findOperable(id);
        application.setUsable(CommonConst.YES.equals(application.getUsable()) ? CommonConst.NO : CommonConst.YES);
        if (!applicationService.updateById(application)) {
            throw new CommonException("更新失败 数据已经变更");
        }
    }

    private Application findOperable(Long id) {
        Application application = findById(id);
        if (!CommonConst.YES.equals(application.getOperable())) {
            throw new CommonException("内置应用不可操作");
        }
        return application;
    }

    private ApplicationResponse toResponse(Application application, String tenantCode) {
        ApplicationResponse response = BeanUtil.copyProperties(application, ApplicationResponse.class);
        response.setClientId(clientId(tenantCode, application.getCode()));
        response.setRedirectUris(splitRedirectUris(application.getRedirectUris()));
        response.setSecretConfigured(StrUtil.isNotBlank(application.getSsoSecret()));
        return response;
    }

    private String normalizeRedirectUris(List<String> redirectUris) {
        if (redirectUris == null) {
            return null;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String redirectUri : redirectUris) {
            String value = StrUtil.trim(redirectUri);
            if (StrUtil.isBlank(value)) {
                continue;
            }
            if (value.contains("*")) {
                throw new CommonException("回调地址不允许使用通配符");
            }
            validateHttpUrl(value, "回调地址");
            values.add(value);
        }
        return String.join(",", values);
    }

    private List<String> splitRedirectUris(String redirectUris) {
        return StrUtil.isBlank(redirectUris) ? List.of() : List.of(redirectUris.split(","));
    }

    private void validateSsoConfiguration(Application application) {
        if (StrUtil.isBlank(application.getSsoSecret())) {
            throw new CommonException("请先生成SSO客户端密钥");
        }
        if (StrUtil.isBlank(application.getRedirectUris())) {
            throw new CommonException("启用SSO前至少配置一个回调地址");
        }
        validateHttpUrl(application.getSsoPushUrl(), "SSO推送地址");
        // 启用时主动验证主密钥，避免直到首次登录才暴露配置错误。
        secretCipher.decrypt(application.getSsoSecret());
    }

    private void validateHttpUrl(String value, String name) {
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || StrUtil.isBlank(uri.getHost())) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new CommonException(name + "格式不正确");
        }
    }

    public static String clientId(String tenantCode, String applicationCode) {
        return tenantCode + ":" + applicationCode;
    }
}
