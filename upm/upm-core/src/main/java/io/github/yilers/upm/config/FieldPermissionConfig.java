package io.github.yilers.upm.config;

import io.github.yilers.api.field.*;
import io.github.yilers.upm.response.UserManagementResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 只登记用户管理专用VO，不给User实体、登录或个人中心响应增加字段限制。 */
@Configuration(proxyBeanMethods = false)
public class FieldPermissionConfig {
    @Bean
    public FieldDefinitionProvider upmFieldDefinitions() {
        return () -> FieldDefinition.fromResponse(UserManagementResponse.class);
    }
}
