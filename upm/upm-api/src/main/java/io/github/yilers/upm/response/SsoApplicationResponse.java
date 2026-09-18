package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "SSO登录页展示的租户及应用公开信息")
public class SsoApplicationResponse {
    @Schema(description = "完整SSO客户端标识", example = "yilers.com:oa")
    private String clientId;

    @Schema(description = "租户编码", example = "yilers.com")
    private String tenantCode;

    @Schema(description = "租户名称", example = "Yilers科技")
    private String tenantName;

    @Schema(description = "租户Logo地址")
    private String tenantLogo;

    @Schema(description = "应用编码", example = "oa")
    private String applicationCode;

    @Schema(description = "应用名称", example = "OA办公系统")
    private String applicationName;

    @Schema(description = "应用Logo地址")
    private String applicationLogo;

    @Schema(description = "应用首页地址", example = "https://oa.example.com")
    private String homeUrl;
}
