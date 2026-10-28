package io.github.yilers.shortlink.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** 复用宿主的MyBatis Plus拦截器；独立部署时宿主也必须提供租户插件。 */
@Configuration
@MapperScan("io.github.yilers.shortlink.mapper")
public class ShortlinkMybatisConfig {
}
