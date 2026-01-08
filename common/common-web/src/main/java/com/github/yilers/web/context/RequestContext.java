package com.github.yilers.web.context;

import lombok.Data;

import java.util.Map;

@Data
public class RequestContext {

    private Long tenantId;

    /**
     * @see com.github.yilers.core.enums.DeviceTypeEnum
     */
    private String deviceType;

    private String traceId;

    private Long userId;

    private Map<String, Object> extra;
}
