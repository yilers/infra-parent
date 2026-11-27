package io.github.yilers.notice.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 仅扫描通知Mapper；租户、分页、乐观锁拦截器由宿主统一提供。 */
@Configuration
@MapperScan("io.github.yilers.notice.mapper")
@EnableConfigurationProperties(NoticeProperties.class)
public class NoticeConfiguration {
    /** 宿主缺少覆盖通知表的租户插件时直接拒绝启动，不能仅凭文档约定保护数据。 */
    @Bean
    public SmartInitializingSingleton noticeTenantIsolationValidator(MybatisPlusInterceptor interceptor) {
        return () -> {
            boolean covered =
                    interceptor.getInterceptors().stream()
                            .filter(item -> item instanceof TenantLineInnerInterceptor)
                            .map(item -> (TenantLineInnerInterceptor) item)
                            .anyMatch(item ->
                                    !item.getTenantLineHandler().ignoreTable("notice_message")
                                            && !item.getTenantLineHandler().ignoreTable("notice_mail_recipient"));
            if (!covered) throw new IllegalStateException("Notice租户拦截器未加入MyBatis-Plus插件链");
        };
    }
}
