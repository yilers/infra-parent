package com.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.yilers.web.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_user")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseAllColumnDomain<User> {

    @Schema(description = "用户类型 admin / user")
    private String userType;
    @Schema(description = "账号")
    private String account;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "密码")
    private String password;
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

    @Schema(description = "扩展字段")
    private String expand;

}
