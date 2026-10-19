package io.github.yilers.flow.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** Flow 自身的查询Mapper注册，不要求宿主扫描UPM或引擎内部包。 */
@Configuration
@MapperScan({"io.github.yilers.flow.mapper", "io.github.yilers.flow.leave.mapper"})
public class FlowQueryConfig {
}
