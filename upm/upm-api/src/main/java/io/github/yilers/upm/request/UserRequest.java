package io.github.yilers.upm.request;

import io.github.yilers.api.validated.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Setter;
import lombok.AccessLevel;

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

    /** 由反序列化setter记录字段是否实际提交，不能由请求正文自行指定。 */
    @JsonIgnore
    @Schema(hidden = true)
    @Setter(AccessLevel.NONE)
    private boolean phoneProvided;
    @JsonIgnore
    @Schema(hidden = true)
    @Setter(AccessLevel.NONE)
    private boolean emailProvided;

    public void setPhone(String phone) {
        this.phone = phone;
        this.phoneProvided = true;
    }
    public void setEmail(String email) {
        this.email = email;
        this.emailProvided = true;
    }
    @Schema(description = "可用状态 1-可用 0-禁用")
    private Integer usable;

    @Schema(description = "角色id列表")
    private List<Long> roleIdList;

    @NotNull(message = "版本号不能为空", groups = Update.class)
    @Schema(description = "乐观锁版本号，修改时必填")
    private Integer version;

}
