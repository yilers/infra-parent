package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 租户账号密码登录策略。
 *
 * <p>该表不使用逻辑删除：记录存在表示启用限制，物理删除后表示该租户不限制密码失败次数。</p>
 */
@Data
@TableName("upm_tenant_login_policy")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户账号密码登录策略")
public class TenantLoginPolicy extends Model<TenantLoginPolicy> {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "策略ID")
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "统计周期内允许的最大连续失败次数", example = "3")
    private Integer maxFailures;

    @Schema(description = "登录失败统计周期，单位分钟", example = "10")
    private Integer failureWindowMinutes;

    @Schema(description = "达到失败上限后的锁定时长，单位分钟", example = "10")
    private Integer lockDurationMinutes;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
