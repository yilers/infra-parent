package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.model.WorkflowBusinessReference;
import io.github.yilers.flow.service.WorkflowBusinessProvider;
import io.github.yilers.flow.i18n.FlowMessage;
import io.github.yilers.web.exception.CommonException;

import java.util.List;
import java.util.Objects;

/** 通用流程只识别业务提交任务的关联，不了解具体业务表和表单字段。 */
final class WorkflowBusinessTasks {

    private WorkflowBusinessTasks() { }

    static WorkflowBusinessReference submission(List<WorkflowBusinessProvider> providers, Long instanceId, String nodeCode) {
        if (nodeCode == null || nodeCode.isBlank()) {
            return null;
        }
        var references = providers.stream().map(provider -> provider.findSubmissionTask(instanceId, nodeCode))
                .filter(Objects::nonNull).toList();
        if (references.size() > 1) {
            throw new CommonException(FlowMessage.BUSINESS_MULTIPLE_ENTRIES);
        }
        return references.isEmpty() ? null : references.getFirst();
    }
}
