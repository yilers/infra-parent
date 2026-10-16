package io.github.yilers.flow.model;

import java.util.List;

/** 流程定义分页结果。 */
public record WorkflowDefinitionPage(long total, List<WorkflowDefinitionInfo> records) {
}
