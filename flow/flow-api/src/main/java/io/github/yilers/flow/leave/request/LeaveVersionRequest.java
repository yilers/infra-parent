package io.github.yilers.flow.leave.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 提交或删除时携带当前草稿版本，避免其他窗口修改后仍操作旧内容。 */
@Schema(description = "请假草稿版本请求")
public record LeaveVersionRequest(@NotNull @Min(1) @Schema(description = "当前草稿版本号") Integer version) {
}
