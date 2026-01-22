package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_role_dept")
@EqualsAndHashCode(callSuper = true)
public class RoleDept extends Model<RoleDept> {

    private Long roleId;
    private Long deptId;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
}
