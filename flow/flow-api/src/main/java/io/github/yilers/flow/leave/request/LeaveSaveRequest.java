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
@Schema(description = "保存请假草稿")
public record LeaveSaveRequest(
        @NotBlank @Schema(description = "请假类型编码") String leaveType,
        @NotNull @Schema(description = "开始时间，yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @NotNull @Schema(description = "结束时间，yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        @NotNull @DecimalMin("0.5") @DecimalMax("8760")
        @Schema(description = "请假小时数，0.5的整数倍，不超过时间区间自然时长") BigDecimal durationHours,
        @NotBlank @Size(max = 1000) @Schema(description = "请假原因，最多1000字") String reason,
        @Schema(description = "编辑时必填，新增时忽略") Integer version) {
}
