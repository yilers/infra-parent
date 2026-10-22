package io.github.yilers.flow.model;

import java.util.List;

/** 不依赖 MyBatis-Plus 分页类型的 Flow 目录分页结果。 */
public record WorkflowDirectoryPage(long current, long size, long total, List<WorkflowDirectoryEntry> records) {
    public WorkflowDirectoryPage {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
