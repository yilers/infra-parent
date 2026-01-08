package com.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_user_role")
@EqualsAndHashCode(callSuper = true)
public class UserRole extends Model<UserRole> {

    private Long userId;
    private Long roleId;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
}
