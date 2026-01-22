package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class UserUpdatePwdRequest {

    @NotBlank(message = "旧秘密不能为空")
    @Schema(description = "旧秘密")
    private String oldPwd;

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码")
    private String newPwd;

    @Schema(hidden = true)
    private Long userId;
}
