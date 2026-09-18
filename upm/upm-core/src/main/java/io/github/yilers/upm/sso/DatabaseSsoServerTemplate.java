package io.github.yilers.upm.sso;

import cn.dev33.satoken.sso.config.SaSsoClientModel;
import cn.dev33.satoken.sso.template.SaSsoServerTemplate;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 将 Sa-Token 默认的静态 Client 配置替换为 UPM 应用表。
 */
@RequiredArgsConstructor
public class DatabaseSsoServerTemplate extends SaSsoServerTemplate {
    private final SsoClientResolver clientResolver;

    @Override
    public SaSsoClientModel getClient(String client) {
        return clientResolver.resolveEnabledModel(client);
    }

    @Override
    public List<SaSsoClientModel> getClients() {
        return clientResolver.findEnabledModels();
    }
}
