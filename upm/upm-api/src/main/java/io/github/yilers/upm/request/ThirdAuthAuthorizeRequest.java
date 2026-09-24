package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发起第三方账号绑定授权请求。
 */
@Data
@Schema(description = "发起第三方账号绑定授权请求")
public class ThirdAuthAuthorizeRequest {

    @NotBlank(message = "认证平台不能为空")
    @Schema(description = "第三方认证平台编码", example = "dingTalk")
    private String platform;
}
