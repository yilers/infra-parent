package io.github.yilers.web.context;

import io.github.yilers.core.enums.DeviceTypeEnum;
import lombok.Data;

import java.util.Map;

@Data
public class RequestContext {

    private Long tenantId;

    /**
     * @see DeviceTypeEnum
     */
    private String deviceType;

    private String traceId;

    private Long userId;

    private Map<String, Object> extra;
}
