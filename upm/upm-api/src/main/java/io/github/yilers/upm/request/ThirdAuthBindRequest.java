package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 完成第三方账号绑定请求。
 */
@Data
@Schema(description = "完成第三方账号绑定请求")
public class ThirdAuthBindRequest {

    @NotBlank(message = "授权码不能为空")
    @Schema(description = "第三方平台回调返回的一次性授权码")
    private String authCode;

    @NotBlank(message = "state不能为空")
    @Schema(description = "UPM生成的一次性OAuth2 state")
    private String state;
}
