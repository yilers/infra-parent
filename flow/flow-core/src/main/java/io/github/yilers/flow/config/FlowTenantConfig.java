package io.github.yilers.flow.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import io.github.yilers.web.context.RequestContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.dromara.warm.flow.core.handler.TenantHandler;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flow 表的租户隔离配置。
 * <p>
 * Warm-Flow 原生表使用 VARCHAR(40) 租户ID，与 UPM 表的 BIGINT 类型不同。
 * 两类表各由一个租户处理器负责，避免 PostgreSQL 出现字符串与数字比较错误。
 */
@Configuration
public class FlowTenantConfig {

    /**
     * 引擎创建流程实体时使用的租户ID，必须与 SQL 租户条件一致。
     */
    @Bean
    public TenantHandler flowTenantHandler() {
        return this::currentTenantId;
    }

    @Bean
    public TenantLineInnerInterceptor flowTenantLineInnerInterceptor() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new StringValue(currentTenantId());
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return tableName == null || !tableName.regionMatches(true, 0, "flow_", 0, 5);
            }
        });
    }

    /**
     * 防止宿主只注册了 Flow 租户处理器 Bean，却忘记将其加入 MyBatis-Plus 插件链。
     * 独立部署时必须提供包含此拦截器的 MybatisPlusInterceptor。
     */
    @Bean
    public SmartInitializingSingleton flowTenantIsolationValidator(
            MybatisPlusInterceptor mybatisPlusInterceptor,
            @Qualifier("flowTenantLineInnerInterceptor") TenantLineInnerInterceptor flowInterceptor) {
        return () -> {
            if (!mybatisPlusInterceptor.getInterceptors().contains(flowInterceptor)) {
                throw new IllegalStateException("Flow 租户拦截器未加入 MyBatis-Plus 插件链");
            }
        };
    }

    private String currentTenantId() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Flow 租户上下文不能为空");
        }
        return tenantId.toString();
    }
}
