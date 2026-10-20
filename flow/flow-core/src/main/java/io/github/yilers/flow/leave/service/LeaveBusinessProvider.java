package io.github.yilers.flow.leave.service;

import io.github.yilers.flow.leave.mapper.LeaveMapper;
import io.github.yilers.flow.model.WorkflowBusinessReference;
import io.github.yilers.flow.service.WorkflowBusinessProvider;
import io.github.yilers.web.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** 只负责业务关联，避免通用审批中心硬编码请假表或通过客户端URL拼接业务入口。 */
@Component
@ConditionalOnProperty(prefix = "flow.leave", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LeaveBusinessProvider implements WorkflowBusinessProvider {
    private final LeaveMapper mapper;
    private final LeaveApplicationTasks applicationTasks;

    @Override
    public WorkflowBusinessReference findReference(Long instanceId) {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Flow租户上下文不能为空");
        }
        Long id = mapper.idByInstance(instanceId, tenantId.toString());
        return id == null ? null : new WorkflowBusinessReference("leave", id, "请假申请");
    }

    @Override
    public WorkflowBusinessReference findSubmissionTask(Long instanceId, String nodeCode) {
        WorkflowBusinessReference reference = findReference(instanceId);
        return reference != null && applicationTasks.isSubmissionNode(instanceId, nodeCode) ? reference : null;
    }
}
