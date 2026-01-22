package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_role_column")
@EqualsAndHashCode(callSuper = true)
public class RoleColumn extends Model<RoleColumn> {

    private Long roleId;

    private String tableName;

    @Schema(description = "忽略字段 多个用逗号分隔")
    private String ignoreColumn;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

}
