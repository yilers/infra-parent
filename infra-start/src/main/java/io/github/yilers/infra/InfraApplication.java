package io.github.yilers.infra;

import cn.hutool.v7.extra.spring.SpringUtil;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import io.github.yilers.auth.EnableAuth;
import io.github.yilers.redisson.EnableRedisson;
import io.github.yilers.web.exception.EnableGlobalException;
import org.dromara.dynamictp.spring.annotation.EnableDynamicTp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.TimeZone;

/**
 * Infra 基础服务统一启动入口。
 *
 * @author hui.zhang
 */
@EnableAuth
@EnableAsync
@EnableRedisson
@EnableDynamicTp
@EnableScheduling
@EnableGlobalException
@EnableResilientMethods
@EnableTransactionManagement
@EnableMethodCache(basePackages = "io.github")
@SpringBootApplication(scanBasePackages = "io.github")
@Import(SpringUtil.class)
public class InfraApplication {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        SpringApplication.run(InfraApplication.class, args);
    }
}
