package io.github.yilers.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 忽略鉴权配置
 * @author zhanghui
 * @since 2023/8/2 17:02
 */

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "ignore")
public class IgnoreProperties {
    private List<String> url = new ArrayList<>();

}
