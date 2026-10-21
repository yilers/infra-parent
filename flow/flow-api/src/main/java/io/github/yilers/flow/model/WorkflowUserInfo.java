package io.github.yilers.flow.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** Flow 使用的最小用户信息，不暴露 UPM 实体和账号敏感字段。 */
@Schema(description = "流程用户展示信息")
public record WorkflowUserInfo(
        @Schema(description = "用户ID") String id,
        @Schema(description = "当前展示姓名") String name) {
}
