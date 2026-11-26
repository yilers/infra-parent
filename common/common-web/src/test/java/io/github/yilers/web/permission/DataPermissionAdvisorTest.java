package io.github.yilers.web.permission;

import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.aop.config.AopConfigUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 验证Advisor经Spring自动代理注册后，接口注解仍能建立真实参数上下文。 */
class DataPermissionAdvisorTest {

    @AfterEach
    void clear() {
        DataPermissionContextHolder.clear();
    }

    @Test
    void springAutomaticallyAppliesInterfacePermission() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            assertEquals(List.of(20L, 30L), context.getBean(ScopeApi.class).query(List.of(20L, 30L)));
            assertNull(DataPermissionContextHolder.get());
        }
    }

    @Test
    void classProxyAlsoPreservesInterfaceAnnotationAndParamName() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(TestConfig.class);
            context.addBeanFactoryPostProcessor(factory -> factory
                    .getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)
                    .getPropertyValues().add("proxyTargetClass", true));
            context.refresh();
            assertEquals(List.of(40L), context.getBean(ScopeApi.class).query(List.of(40L)));
            assertNull(DataPermissionContextHolder.get());
        }
    }

    public interface ScopeApi {
        @DataPermission(tableName = "biz_order", columnName = "project_id", columnValue = "#allowed")
        List<Long> query(@Param("allowed") List<Long> input);
    }

    public static class ScopeService implements ScopeApi {
        @Override
        public List<Long> query(List<Long> input) {
            return DataPermissionContextHolder.get().getFirst().getColumnValues();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        DataPermissionAspect dataPermissionAspect() { return new DataPermissionAspect(); }

        @Bean
        ScopeApi scopeService() { return new ScopeService(); }
    }
}
