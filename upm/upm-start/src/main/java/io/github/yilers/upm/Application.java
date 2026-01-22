package io.github.yilers.upm;

import cn.hutool.v7.extra.spring.SpringUtil;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import io.github.yilers.auth.EnableAuth;
import io.github.yilers.redisson.EnableRedisson;
import io.github.yilers.web.exception.EnableGlobalException;
import org.dromara.dynamictp.spring.annotation.EnableDynamicTp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.TimeZone;

/**
 * 启动类
 * @author hui.zhang
 * @since 2021/1/20 2:09 下午
 */

@EnableAuth
@EnableAsync
@EnableRedisson
@EnableDynamicTp
@EnableScheduling
//@EnableAdminServer
@EnableGlobalException
@EnableTransactionManagement
@EnableMethodCache(basePackages = "io.github")
@SpringBootApplication(scanBasePackages = {"io.github"})
@Import(value = {SpringUtil.class})
public class Application {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}


}

