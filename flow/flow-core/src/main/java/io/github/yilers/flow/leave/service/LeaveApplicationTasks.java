package io.github.yilers.flow.leave.service;

import io.github.yilers.flow.mapper.WorkflowInboxMapper;
import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.flow.i18n.FlowMessage;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.ActivityStatus;
import org.dromara.warm.flow.core.enums.NodeType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 请假申请节点识别与授权。唯一的、仅配置“动态规则：申请人”的普通节点承担表单提交职责。
 * 不按节点名称判断，不把主管节点恰好分配给申请人的情况当成申请提交。
 */
@Component
@ConditionalOnProperty(prefix = "flow.leave", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LeaveApplicationTasks {

    private final WorkflowInboxMapper inboxMapper;
    private final WorkflowIdentityProvider identityProvider;

    public boolean isSubmissionNode(Long instanceId, String nodeCode) {
        return nodeCode != null && nodeCode.equals(submissionNode(instance(instanceId)));
    }

    /** 首次提交仅自动完成开始后的唯一申请任务，不循环跳过后续任务或主管审批。 */
    public Task initialTask(Long instanceId) {
        Instance instance = instance(instanceId);
        String nodeCode = submissionNode(instance);
        if (nodeCode == null) {
            return null;
        }
        Task task = currentTask(instance, nodeCode);
        if (task == null) {
            throw new CommonException(FlowMessage.LEAVE_START_NODE);
        }
        return task;
    }

    /** 用于展示；没有当前申请任务或实例已挂起时，不允许编辑已提交单据。 */
    public Task currentTask(Long instanceId) {
        Instance instance = instance(instanceId);
        return currentTask(instance, submissionNode(instance));
    }

    /** 先锁流程实例再锁业务单据，与通用审批使用相同的实例串行化边界。 */
    public Task lockCurrentTask(Long instanceId, Long taskId, String userId) {
        if (!Integer.valueOf(java.sql.Connection.TRANSACTION_READ_COMMITTED)
                .equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())) {
            throw new CommonException(FlowMessage.LEAVE_TRANSACTION);
        }
        if (taskId == null || taskId <= 0 || !instanceId.equals(inboxMapper.lockInstanceByTask(taskId, tenantId()))) {
            throw changed();
        }
        Task task = currentTask(instanceId);
        if (task == null || !taskId.equals(task.getId()) || !canHandle(taskId, userId)) {
            throw changed();
        }
        return task;
    }

    public boolean canHandle(Long taskId, String userId) {
        List<String> flags = identityProvider.currentPermissionFlags();
        return flags != null && !flags.isEmpty() && inboxMapper.actionableTaskCount(taskId, tenantId(), userId, flags) == 1;
    }

    private Task currentTask(Instance instance, String nodeCode) {
        if (nodeCode == null || !ActivityStatus.ACTIVITY.getKey().equals(instance.getActivityStatus())) {
            return null;
        }
        var definition = FlowEngine.defService().getById(instance.getDefinitionId());
        if (definition == null || !ActivityStatus.ACTIVITY.getKey().equals(definition.getActivityStatus())) {
            return null;
        }
        List<Task> tasks = FlowEngine.taskService().list(FlowEngine.newTask().setInstanceId(instance.getId()));
        return tasks.size() == 1 && nodeCode.equals(tasks.getFirst().getNodeCode()) ? tasks.getFirst() : null;
    }

    private String submissionNode(Instance instance) {
        var nodes = FlowEngine.nodeService().list(FlowEngine.newNode().setDefinitionId(instance.getDefinitionId())).stream()
                .filter(node -> NodeType.BETWEEN.getKey().equals(node.getNodeType())
                        && WorkflowAssigneeRule.APPLICANT.expression().equals(node.getPermissionFlag())).toList();
        if (nodes.size() > 1) {
            throw new CommonException(FlowMessage.LEAVE_SINGLE_SUBMISSION);
        }
        return nodes.isEmpty() ? null : nodes.getFirst().getNodeCode();
    }

    private Instance instance(Long instanceId) {
        Instance instance = FlowEngine.insService().getById(instanceId);
        if (instance == null || !tenantId().equals(instance.getTenantId())) {
            throw new CommonException(FlowMessage.LEAVE_FLOW_UNAVAILABLE);
        }
        return instance;
    }

    private String tenantId() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new CommonException(FlowMessage.TENANT_REQUIRED);
        }
        return tenantId.toString();
    }

    private CommonException changed() {
        return new CommonException(FlowMessage.LEAVE_TASK_CHANGED);
    }
}
