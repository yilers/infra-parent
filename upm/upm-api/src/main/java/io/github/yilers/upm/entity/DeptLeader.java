package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 部门主管任职记录。endDate 为不包含的结束日期，空值表示仍在任。
 */
@Data
@TableName("upm_dept_leader")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "部门主管任职记录")
public class DeptLeader extends Model<DeptLeader> {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "任职记录ID")
    private Long id;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "主管用户ID")
    private Long userId;

    @Schema(description = "主管类型：1-主管，2-副主管")
    private Integer leaderType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "任职开始日期，包含当天")
    private LocalDate startDate;

    @Schema(description = "任职结束日期，不包含当天；空值表示仍在任")
    private LocalDate endDate;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    @Schema(description = "租户ID")
    private Long tenantId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
