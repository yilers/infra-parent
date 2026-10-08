package io.github.yilers.upm.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.sso.SaSsoManager;
import cn.dev33.satoken.sso.config.SaSsoServerConfig;
import cn.dev33.satoken.sso.processor.SaSsoServerProcessor;
import cn.dev33.satoken.sso.template.SaSsoServerTemplate;
import io.github.yilers.upm.handler.SsoHandler;
import io.github.yilers.upm.sso.DatabaseSsoServerTemplate;
import io.github.yilers.upm.sso.SsoClientResolver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Sa-Token SSO Server 配置。客户端清单来自 UPM 应用表，不使用静态配置。
 */
@Configuration
public class UpmSsoConfig {

    @Bean
    public SaSsoServerTemplate ssoServerTemplate(SsoClientResolver clientResolver) {
        return new DatabaseSsoServerTemplate(clientResolver);
    }

    @Bean
    public InitializingBean configureSsoServer(SaSsoServerTemplate template, SsoHandler ssoHandler,
                                               @Value("${upm.sso.ticket-timeout:60}") long ticketTimeout) {
        return () -> {
            SaSsoServerConfig config = new SaSsoServerConfig()
                    .setTicketTimeout(ticketTimeout)
                    .setAllowAnonClient(false)
                    .setIsCheckSign(true)
                    .setIsSlo(true)
                    .setAutoRenewTimeout(true);
            config.setMode("ticket");
            SaSsoManager.setServerConfig(config);

            // Sa-Token 默认 HTTP 处理器不发送请求，单点注销时需向客户端推送签名消息。
            RestClient restClient = RestClient.create();
            template.strategy.sendRequest = url ->
                    restClient.get().uri(url).retrieve().body(String.class);

            // ticket 校验已经由 Sa-Token 完成签名和一次性消费，这里仅追加可信业务上下文。
            template.strategy.checkTicketAppendData = (loginId, result) -> {
                String client = SaHolder.getRequest().getParam(template.paramName.client);
                String tokenValue = result.get(template.paramName.tokenValue, String.class);
                result.set("upmContext", ssoHandler.buildLoginContext(Long.valueOf(loginId.toString()), client, tokenValue));
                return result;
            };
            SaSsoServerProcessor.instance.ssoServerTemplate = template;
        };
    }
}
