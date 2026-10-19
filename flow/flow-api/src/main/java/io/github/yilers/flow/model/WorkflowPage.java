package io.github.yilers.flow.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 审批中心统一分页结果，不暴露引擎和持久层分页类型。 */
@Schema(description = "审批中心分页结果")
public record WorkflowPage(@Schema(description = "总条数") long total,
                           @Schema(description = "当前页记录") List<WorkflowWorkItem> records) {
}
