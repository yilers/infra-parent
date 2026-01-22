package io.github.yilers.upm.request;

import io.github.yilers.core.enums.DeviceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登陆DTO
 * @author hui.zhang
 * @since 2021/1/29 10:10 上午
 **/

@Data
public class LoginRequest implements Serializable {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "账号")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;

    @Schema(description = "设备类型 web h5 vx ali")
    private String device = DeviceTypeEnum.WEB.getCode();

}
