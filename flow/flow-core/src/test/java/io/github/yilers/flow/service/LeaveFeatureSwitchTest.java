package io.github.yilers.flow.service;

import io.github.yilers.flow.leave.controller.LeaveController;
import io.github.yilers.flow.leave.service.LeaveBusinessProvider;
import io.github.yilers.flow.leave.service.LeaveService;
import io.github.yilers.flow.leave.service.LeaveServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 不启用请假示例时，通用Flow宿主无需提供该业务的Mapper、Service或数据库表。 */
class LeaveFeatureSwitchTest {

    @ParameterizedTest
    @ValueSource(strings = {"false", "unset"})
    void doesNotLoadBusinessEndpointsUnlessExplicitlyEnabled(String value) {
        try (var context = new AnnotationConfigApplicationContext()) {
            if (!value.equals("unset")) {
                context.getEnvironment().getPropertySources().addFirst(
                        new MapPropertySource("test", Map.of("flow.leave.enabled", value)));
            }
            context.register(LeaveServiceImpl.class, LeaveBusinessProvider.class, LeaveController.class,
                    io.github.yilers.flow.leave.service.LeaveApplicationTasks.class);
            context.refresh();
            assertTrue(context.getBeansOfType(LeaveService.class).isEmpty());
            assertTrue(context.getBeansOfType(WorkflowBusinessProvider.class).isEmpty());
            assertTrue(context.getBeansOfType(LeaveController.class).isEmpty());
        }
    }
}
