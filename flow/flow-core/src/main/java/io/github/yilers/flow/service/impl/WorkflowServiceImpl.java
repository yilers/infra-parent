package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.mapper.WorkflowInboxMapper;
import io.github.yilers.flow.service.WorkflowBusinessProvider;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.service.WorkflowService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

/**
 * Warm-Flow 1.8.9 适配实现。使用引擎的 pass/reject API，办理人权限由
 * {@link io.github.yilers.flow.config.FlowPermissionConfig} 统一提供。
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowIdentityProvider identityProvider;
    private final WorkflowInboxMapper inboxMapper;
    private final WorkflowAssigneeResolver assigneeResolver;
    private final List<WorkflowBusinessProvider> businessProviders;

    public WorkflowServiceImpl(WorkflowIdentityProvider identityProvider, WorkflowInboxMapper inboxMapper,
                               WorkflowAssigneeResolver assigneeResolver, List<WorkflowBusinessProvider> businessProviders) {
        this.identityProvider = identityProvider;
        this.inboxMapper = inboxMapper;
        this.assigneeResolver = assigneeResolver;
        this.businessProviders = businessProviders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(String flowCode, String businessId, Map<String, Object> variables) {
        requireText(flowCode, "流程编码");
        requireText(businessId, "业务单据ID");
        String applicantId = identityProvider.currentUserId();
        FlowParams params = FlowParams.build().flowCode(flowCode).flowStatus(FlowStatus.APPROVAL.getKey())
                .variable(assigneeResolver.prepare(flowCode, applicantId, variables));
        Instance instance = FlowEngine.insService().start(businessId, params);
        validateAssignees(instance.getId());
        return instance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long pass(Long taskId, String message, Map<String, Object> variables) {
        requireTaskId(taskId);
        requireApprovalTask(lockAndAuthorize(taskId));
        Long instanceId = FlowEngine.taskService().pass(taskId, message, assigneeResolver.copyBusinessVariables(variables)).getId();
        validateAssignees(instanceId);
        return instanceId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long reject(Long taskId, String message, Map<String, Object> variables) {
        requireTaskId(taskId);
        requireApprovalTask(lockAndAuthorize(taskId));
        Long instanceId = FlowEngine.taskService().reject(taskId, message, assigneeResolver.copyBusinessVariables(variables)).getId();
        validateAssignees(instanceId);
        return instanceId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long submitBusinessTask(Long taskId, String businessType, String message, Map<String, Object> variables) {
        requireTaskId(taskId);
        Task task = lockAndAuthorize(taskId);
        var reference = WorkflowBusinessTasks.submission(businessProviders, task.getInstanceId(), task.getNodeCode());
        if (reference == null || !reference.type().equals(businessType)) {
            throw new CommonException("该任务不是指定业务的申请提交任务");
        }
        Long instanceId = FlowEngine.taskService().pass(taskId, message, assigneeResolver.copyBusinessVariables(variables)).getId();
        validateAssignees(instanceId);
        return instanceId;
    }

    private void requireApprovalTask(Task task) {
        if (WorkflowBusinessTasks.submission(businessProviders, task.getInstanceId(), task.getNodeCode()) != null) {
            throw new CommonException("这是申请提交任务，请从业务表单修改或重新提交，不能使用审批通过或退回");
        }
    }

    /**
     * 行锁属于当前业务事务，跨节点部署也有效；锁持有到业务与流程一起提交。
     * 多人同时点击同一任务时，后到请求必须重新检查任务和候选人。
     */
    private Task lockAndAuthorize(Long taskId) {
        if (!Integer.valueOf(java.sql.Connection.TRANSACTION_READ_COMMITTED)
                .equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())) {
            throw new CommonException("流程审批必须在READ_COMMITTED事务中执行，请检查业务外层事务配置");
        }
        String userId = identityProvider.currentUserId();
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new CommonException("Flow租户上下文不能为空");
        }
        if (inboxMapper.lockInstanceByTask(taskId, tenantId.toString()) == null) {
            throw new CommonException("任务已处理、不存在或无权办理，请刷新待办");
        }
        List<String> flags = identityProvider.currentPermissionFlags();
        if (flags == null || flags.isEmpty()
                || inboxMapper.actionableTaskCount(taskId, tenantId.toString(), userId, flags) != 1) {
            throw new CommonException("任务已处理、不存在或无权办理，请刷新待办");
        }
        Task task = FlowEngine.taskService().getById(taskId);
        if (task == null) {
            throw new CommonException("任务已处理、不存在或无权办理，请刷新待办");
        }
        return task;
    }

    /**
     * 引擎的办理人转换异常会被内部捕获，因此落库后仍需检查转换结果。
     * 未解析的角色/部门、空办理人均回滚，避免生成无人可办或不安全的任务。
     */
    private void validateAssignees(Long instanceId) {
        FlowEngine.taskService().list(FlowEngine.newTask().setInstanceId(instanceId)).forEach(task -> {
            var users = FlowEngine.userService().listByAssociatedAndTypes(task.getId());
            if (users == null || users.isEmpty() || users.stream().anyMatch(user ->
                    user.getProcessedBy() == null || user.getProcessedBy().isBlank()
                            || user.getProcessedBy().startsWith("user:")
                            || user.getProcessedBy().startsWith("role:")
                            || user.getProcessedBy().contains("${")
                            || user.getProcessedBy().startsWith("dept:"))) {
                throw new CommonException("节点“" + task.getNodeName() + "”未解析出有效审批人，请检查办理人配置");
            }
        });
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
    }

    private static void requireTaskId(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("任务ID必须为正数");
        }
    }
}
