package io.github.yilers.flow.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 当前租户中与本人相关的实例详情，不包含业务正文和原始流程变量。 */
@Schema(description = "流程实例详情")
public record WorkflowInstanceDetail(
        @Schema(description = "流程实例摘要") WorkflowWorkItem instance,
        @Schema(description = "当前节点任务，actionable标记本人可办理任务") List<WorkflowWorkItem> tasks,
        @Schema(description = "审批历史，按办理时间升序") List<WorkflowWorkItem> history,
        @Schema(description = "脱敏后的运行流程图JSON；只用于展示") String chartJson,
        @Schema(description = "可选业务关联；不直接返回业务正文") WorkflowBusinessReference business) {
}
