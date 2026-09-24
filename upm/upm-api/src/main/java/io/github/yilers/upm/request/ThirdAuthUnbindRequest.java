package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解除第三方账号绑定请求。
 */
@Data
@Schema(description = "解除第三方账号绑定请求")
public class ThirdAuthUnbindRequest {

    @NotBlank(message = "认证平台不能为空")
    @Schema(description = "第三方认证平台编码", example = "dingTalk")
    private String platform;
}
