package io.github.yilers.upm.request;

import io.github.yilers.web.validated.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserRequest {

    @NotNull(message = "id不能为空", groups = Update.class)
    private Long id;

    @NotBlank(message = "账号不能为空")
    @Schema(description = "账号")
    private String account;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "密码")
    private String password;
    @NotNull(message = "部门id为空")
    @Schema(description = "部门id")
    private Long deptId;
    @Schema(description = "职位id")
    private Long positionId;
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "性别 1-男 0-女")
    private Integer gender;
    @Schema(description = "头像")
    private String photo;
    @Schema(description = "身份证号")
    private String idCard;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "手机")
    private String phone;
    @Schema(description = "可用状态 1-可用 0-禁用")
    private Integer usable;

    @Schema(description = "角色id列表")
    private List<Long> roleIdList;

}
