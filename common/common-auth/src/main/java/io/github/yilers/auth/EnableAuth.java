package io.github.yilers.auth;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 开启认证
 * @author zhanghui
 * @since 2023/9/7 11:40
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Configuration
@Import({IgnoreProperties.class, SaTokenConfig.class, StpInterfaceImpl.class})
public @interface EnableAuth {
}
