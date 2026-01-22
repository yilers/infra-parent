package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_role_permission")
@EqualsAndHashCode(callSuper = true)
public class RolePermission extends Model<RolePermission> {

    private Long roleId;
    private Long permissionId;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    private String device;
}
