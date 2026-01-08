package com.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_user_data_scope")
@EqualsAndHashCode(callSuper = true)
public class UserDataScope extends Model<UserDataScope> {

    private Long userId;

    private String interfacePath;

    @Schema(description = "数据权限类型 1-全部 2-本部门及以下 3-本部门 4-自定义部门")
    private Integer dataScope;

    @Schema(description = "扩展字段")
    private String expand;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
}
