package io.github.yilers.upm.request;

import io.github.yilers.core.enums.DeviceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 第三方账号登录授权请求。
 */
@Data
@Schema(description = "第三方账号登录授权请求")
public class ThirdAuthLoginAuthorizeRequest {

    @NotBlank(message = "租户编码不能为空")
    @Schema(description = "租户编码", example = "yilers.com")
    private String tenantCode;

    @NotBlank(message = "第三方认证平台不能为空")
    @Schema(description = "第三方认证平台编码", example = "dingTalk")
    private String platform;

    @NotBlank(message = "登录设备类型不能为空")
    @Schema(description = "登录设备类型", example = "web")
    private String device = DeviceTypeEnum.WEB.getCode();
}
