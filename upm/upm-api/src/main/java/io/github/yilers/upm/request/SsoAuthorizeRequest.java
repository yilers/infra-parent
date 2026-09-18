package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建SSO一次性授权ticket请求")
public class SsoAuthorizeRequest {
    @NotBlank(message = "client不能为空")
    @Size(max = 129)
    @Schema(description = "SSO客户端标识，格式为“租户编码:应用编码”",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "yilers.com:oa")
    private String client;

    @NotBlank(message = "回调地址不能为空")
    @Size(max = 500)
    @Schema(description = "业务应用接收ticket的完整回调地址，必须命中应用配置的回调白名单",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "https://oa.example.com/sso/callback")
    private String redirect;

    @Size(max = 500)
    @Schema(description = "业务应用生成的一次性随机值，回调后必须校验，用于防止登录CSRF",
            example = "530m6ePz1kK9qLrT")
    private String state;
}
