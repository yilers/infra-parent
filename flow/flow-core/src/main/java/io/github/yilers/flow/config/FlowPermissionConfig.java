package io.github.yilers.flow.config;

import io.github.yilers.flow.service.WorkflowIdentityProvider;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 将宿主系统的登录身份接入 Warm-Flow 1.8.9 办理人校验。
 */
@Configuration
public class FlowPermissionConfig {

    @Bean
    public PermissionHandler flowPermissionHandler(WorkflowIdentityProvider identityProvider) {
        return new PermissionHandler() {
            @Override
            public List<String> permissions() {
                return identityProvider.currentPermissionFlags();
            }

            @Override
            public String getHandler() {
                return identityProvider.currentUserId();
            }
        };
    }

    /**
     * 权限处理器未装载时禁止启动，避免流程任务在缺少办理人校验的状态下运行。
     */
    @Bean
    public SmartInitializingSingleton flowPermissionValidator(PermissionHandler flowPermissionHandler) {
        return () -> {
            if (FlowEngine.permissionHandler() != flowPermissionHandler) {
                throw new IllegalStateException("Warm-Flow 未装载项目办理人权限处理器");
            }
        };
    }
}
