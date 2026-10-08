package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 当前登录用户修改个人资料。账号、部门、职位和角色不允许通过此接口修改。
 */
@Data
public class UserProfileUpdateRequest {

    @NotNull(message = "版本号不能为空")
    @Schema(description = "乐观锁版本号，修改时必填")
    private Integer version;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名")
    private String name;

    @NotBlank(message = "昵称不能为空")
    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "性别 1-男 0-女")
    private Integer gender;

    @Schema(description = "头像")
    private String photo;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机")
    private String phone;
}
