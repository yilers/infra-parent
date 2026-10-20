package io.github.yilers.flow.leave.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 只接收业务字段；不允许浏览器指定申请人、租户、流程编码或实例ID。 */
@Schema(description = "保存请假草稿或本人申请提交节点的修改")
public record LeaveSaveRequest(
        @NotBlank @Schema(description = "请假类型编码") String leaveType,
        @NotNull @Schema(description = "开始时间，yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @NotNull @Schema(description = "结束时间，yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        @NotNull @DecimalMin("0.5") @DecimalMax("8760")
        @Schema(description = "请假小时数，0.5的整数倍，不超过时间区间自然时长") BigDecimal durationHours,
        @NotBlank @Size(max = 1000) @Schema(description = "请假原因，最多1000字") String reason,
        @Schema(description = "编辑时必填，新增时忽略") Integer version,
        @Schema(description = "已发起申请的当前提交任务ID；修改退回单据时必填，草稿不传") Long submissionTaskId) {

    /** 保留已有业务Service创建草稿的调用方式。 */
    public LeaveSaveRequest(String leaveType, LocalDateTime startTime, LocalDateTime endTime,
                            BigDecimal durationHours, String reason, Integer version) {
        this(leaveType, startTime, endTime, durationHours, reason, version, null);
    }
}
