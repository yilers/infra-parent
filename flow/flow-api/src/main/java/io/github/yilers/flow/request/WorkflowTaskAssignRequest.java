package io.github.yilers.flow.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 转办和委派请求；目标用户由服务端复核租户和启用状态。 */
@Schema(description = "流程任务转办或委派请求")
public record WorkflowTaskAssignRequest(
        @NotNull(message = "请选择目标用户")
        @Positive(message = "目标用户ID必须为正数")
        @Schema(description = "目标用户ID") Long targetUserId,
        @Size(max = 500, message = "操作说明最多500个字符")
        @Schema(description = "转办或委派说明，最多500个字符") String message) {
}
