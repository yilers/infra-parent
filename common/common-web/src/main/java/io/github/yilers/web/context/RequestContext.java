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

    /** 系统调用凭证，与登录用户身份互斥；不存放密钥。 */
    private Long credentialId;

    private Map<String, Object> extra;
}
