package io.github.yilers.flow.leave.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 请假示例绑定的流程编码。相同编码的定义仍按租户隔离，客户端不可覆盖。 */
@Data
@Component
@ConfigurationProperties(prefix = "flow.leave")
public class LeaveProperties {
    /** 请假示例按宿主显式启用，避免只使用通用引擎的服务被迫依赖业务表。 */
    private boolean enabled;
    private String flowCode = "leave";
}
