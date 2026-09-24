package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 第三方账号登录回调请求。
 */
@Data
@Schema(description = "第三方账号登录回调请求")
public class ThirdAuthLoginRequest {

    @NotBlank(message = "授权码不能为空")
    @Schema(description = "第三方平台返回的一次性授权码")
    private String authCode;

    @NotBlank(message = "认证状态不能为空")
    @Schema(description = "发起登录授权时生成的一次性state")
    private String state;
}
