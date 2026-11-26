package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色字段授权关联；取消授权直接物理删除，由角色版本控制并发保存。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upm_role_field")
public class RoleField extends Model<RoleField> {
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    @Schema(description = "所属租户ID")
    private Long tenantId;
    @Schema(description = "角色ID")
    private Long roleId;
    @Schema(description = "字段目录ID")
    private Long fieldId;
    @Schema(description = "查看方式MASKED/FULL；隐藏表示无授权记录")
    private String viewMode;
    @Schema(description = "是否允许修改")
    private Boolean editable;
}
