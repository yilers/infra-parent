package io.github.yilers.web.powerjob;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import tech.powerjob.client.PowerJobClient;

import java.util.List;

/**
 * powerjob客户端配置
 * @author zhanghui
 * @since 2023/10/10 11:59
 */

@Slf4j
public class PowerJobClientConfig {

    @Value("#{'${powerjob.worker.server-address}'.split(',')}")
    private List<String> domain;

    @Value("${powerjob.worker.app-name}")
    private String appName;

    @Value("${powerjob.worker.password}")
    private String password;

    @Bean
    @ConditionalOnProperty(name = "powerjob.worker.enabled", havingValue = "true")
    public PowerJobClient powerJobClient() {
        // 初始化 client，需要server地址和应用名称作为参数
        PowerJobClient client = new PowerJobClient(domain, appName, password);
        log.info("PowerJob Client Bean注入成功");
        return client;
    }

}
