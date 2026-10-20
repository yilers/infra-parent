package io.github.yilers.flow.leave.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 首次提交草稿与退回后重新提交共用入口，由服务端根据单据状态确定分支。 */
@Schema(description = "提交请假申请或重新提交原流程")
public record LeaveSubmitRequest(
        @NotNull @Min(1) @Schema(description = "当前单据版本号") Integer version,
        @Schema(description = "当前申请提交任务ID；重新提交时必填，首次提交草稿不传") Long submissionTaskId) {
}
