package io.github.yilers.upm.config;

import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import com.baomidou.mybatisplus.extension.parser.cache.JdkSerialCaffeineJsqlParseCache;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import io.github.yilers.upm.permission.CustomDataPermissionHandler;
import io.github.yilers.upm.permission.CustomTenantHandler;
import io.github.yilers.web.mybatis.InsertBatchSqlInjector;
import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@MapperScan({"io.github.yilers.upm.mapper"})
public class MybatisPlusConfig {

    @PostConstruct
    public void initJsqlParserCache() {
        JsqlParserGlobal.setJsqlParseCache(
                new JdkSerialCaffeineJsqlParseCache(
                        cache -> cache.maximumSize(1024) // 建议设置 1000～2000 之间
                                .expireAfterWrite(60, TimeUnit.SECONDS) // SQL 缓存存活 1 分钟
                )
        );
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 列数据权限
//        CustomResultInterceptor customResultInterceptor = new CustomResultInterceptor();
//        interceptor.addInnerInterceptor(customResultInterceptor);
        // 多租户插件
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new CustomTenantHandler());
        interceptor.addInnerInterceptor(tenantInterceptor);
        // 行数据权限
        DataPermissionInterceptor dataPermissionInterceptor = new DataPermissionInterceptor(new CustomDataPermissionHandler());
        interceptor.addInnerInterceptor(dataPermissionInterceptor);
        // 分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 防止全表更新与删除
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    @Bean
    public InsertBatchSqlInjector insertBatchSqlInjector() {
        return new InsertBatchSqlInjector();
    }

}
