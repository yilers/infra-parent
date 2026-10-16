package io.github.yilers.flow.model;

import java.util.Date;

/**
 * 流程定义摘要，不向调用方暴露 Warm-Flow 的实体类型。
 */
public record WorkflowDefinitionInfo(Long id, String flowCode, String flowName, String version,
                                     String category, Integer publishStatus, Integer activityStatus,
                                     Date createTime, Date updateTime) {
}
