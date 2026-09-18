package io.github.yilers.upm.sso;

import cn.dev33.satoken.sso.config.SaSsoClientModel;
import cn.hutool.v7.core.text.StrUtil;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Application;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.handler.ApplicationHandler;
import io.github.yilers.upm.service.ApplicationService;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 UPM 数据库解析 Sa-Token SSO Client，避免在配置文件中重复维护应用。
 */
@Component
@RequiredArgsConstructor
public class SsoClientResolver {
    private final TenantService tenantService;
    private final ApplicationService applicationService;
    private final SsoSecretCipher secretCipher;

    public Client resolve(String clientId) {
        String[] value = splitClientId(clientId);
        try {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            Tenant tenant = tenantService.findByCode(value[0]);
            if (tenant == null || !CommonConst.YES.equals(tenant.getUsable())) {
                throw new CommonException("租户不存在或已停用");
            }
            Application application = applicationService.getOne(Wrappers.<Application>lambdaQuery()
                    .eq(Application::getTenantId, tenant.getId())
                    .eq(Application::getCode, value[1]));
            if (application == null || !CommonConst.YES.equals(application.getUsable())) {
                throw new CommonException("应用不存在或已停用");
            }
            return new Client(clientId, tenant, application);
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    public SaSsoClientModel resolveEnabledModel(String clientId) {
        return toModel(requireEnabled(resolve(clientId)));
    }

    public List<SaSsoClientModel> findEnabledModels() {
        try {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            List<SaSsoClientModel> result = new ArrayList<>();
            for (Application application : applicationService.list(Wrappers.<Application>lambdaQuery()
                    .eq(Application::getSsoEnabled, CommonConst.YES)
                    .eq(Application::getUsable, CommonConst.YES))) {
                Tenant tenant = tenantService.getById(application.getTenantId());
                if (tenant != null && CommonConst.YES.equals(tenant.getUsable())) {
                    result.add(toModel(new Client(ApplicationHandler.clientId(tenant.getCode(), application.getCode()),
                            tenant, application)));
                }
            }
            return result;
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    public Client requireEnabled(Client client) {
        Application application = client.application();
        if (!CommonConst.YES.equals(application.getSsoEnabled())) {
            throw new CommonException("应用未启用单点登录");
        }
        if (StrUtil.isBlank(application.getSsoSecret()) || StrUtil.isBlank(application.getRedirectUris())
                || StrUtil.isBlank(application.getSsoPushUrl())) {
            throw new CommonException("应用单点登录配置不完整");
        }
        return client;
    }

    private SaSsoClientModel toModel(Client client) {
        Application application = client.application();
        URI pushUri = URI.create(application.getSsoPushUrl());
        String serverUrl = pushUri.getScheme() + "://" + pushUri.getRawAuthority();
        String pushUrl = StrUtil.isBlank(pushUri.getRawPath()) ? "/" : pushUri.getRawPath();
        if (StrUtil.isNotBlank(pushUri.getRawQuery())) {
            pushUrl += "?" + pushUri.getRawQuery();
        }
        return new SaSsoClientModel()
                .setClient(client.clientId())
                .setAllowUrl(application.getRedirectUris())
                .setSecretKey(secretCipher.decrypt(application.getSsoSecret()))
                .setServerUrl(serverUrl)
                .setPushUrl(pushUrl)
                .setIsPush(true)
                .setIsSlo(true);
    }

    private String[] splitClientId(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            throw new CommonException("client不能为空");
        }
        int separator = clientId.indexOf(':');
        if (separator <= 0 || separator == clientId.length() - 1 || clientId.indexOf(':', separator + 1) >= 0) {
            throw new CommonException("client格式应为tenantCode:applicationCode");
        }
        return new String[]{clientId.substring(0, separator), clientId.substring(separator + 1)};
    }

    public record Client(String clientId, Tenant tenant, Application application) {
    }
}
