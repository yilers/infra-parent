package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.mapper.WorkflowInboxMapper;
import io.github.yilers.flow.service.WorkflowBusinessProvider;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.service.WorkflowService;
import io.github.yilers.flow.model.WorkflowUserInfo;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.ActivityStatus;
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
    public Long transfer(Long taskId, Long targetUserId, String message) {
        return changeHandler(taskId, targetUserId, message, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long delegate(Long taskId, Long targetUserId, String message) {
        return changeHandler(taskId, targetUserId, message, true);
    }

    /** 转办和委派共用相同的任务锁、办理人校验及目标用户租户校验。 */
    private Long changeHandler(Long taskId, Long targetUserId, String message, boolean delegation) {
        requireTaskId(taskId);
        if (targetUserId == null || targetUserId <= 0) {
            throw new CommonException("目标用户ID必须为正数");
        }
        Task task = lockAndAuthorize(taskId);
        requireApprovalTask(task);
        String currentUserId = identityProvider.currentUserId();
        String targetId = targetUserId.toString();
        if (currentUserId.equals(targetId)) {
            throw new CommonException("不能将任务转给自己");
        }
        // UPM适配器只会返回当前租户内启用用户；缺失即视为无效目标。
        List<String> validTargets = identityProvider.resolveUserIds(List.of("user:" + targetId));
        if (validTargets.size() != 1 || !targetId.equals(validTargets.getFirst())) {
            throw new CommonException("目标用户不存在、已停用或不属于当前租户");
        }
        Map<String, WorkflowUserInfo> users = identityProvider.resolveUsers(List.of(targetId));
        WorkflowUserInfo target = users.get(targetId);
        String targetName = target == null || target.name() == null || target.name().isBlank()
                ? targetId : target.name();
        String actionName = delegation ? "委派给" : "转办给";
        String actionMessage = message == null || message.isBlank()
                ? actionName + targetName : message.trim();
        List<String> flags = identityProvider.currentPermissionFlags();
        FlowParams params = FlowParams.build().handler(currentUserId)
                .permissionFlag(flags == null ? List.of() : flags)
                .message(actionMessage).addHandlers(List.of(targetId));
        boolean changed = delegation
                ? FlowEngine.taskService().depute(taskId, params)
                : FlowEngine.taskService().transfer(taskId, params);
        if (!changed) {
            throw new CommonException(delegation ? "委派失败，请刷新待办后重试" : "转办失败，请刷新待办后重试");
        }
        return task.getInstanceId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long takeBack(Long instanceId) {
        Instance instance = lockOwnedInstance(instanceId);
        requireActiveInstance(instance, List.of(FlowStatus.APPROVAL));
        if (FlowEngine.taskService().getByInsId(instanceId).isEmpty()) {
            throw new CommonException("流程已没有待办任务，不能拿回");
        }
        FlowParams params = applicantActionParams("申请人拿回申请");
        Instance result = FlowEngine.taskService().taskBackByInsId(instanceId, params);
        validateAssignees(result.getId());
        return result.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long revoke(Long instanceId) {
        Instance instance = lockOwnedInstance(instanceId);
        requireActiveInstance(instance, List.of(FlowStatus.APPROVAL, FlowStatus.REJECT, FlowStatus.TASK_BACK));
        if (FlowEngine.taskService().getByInsId(instanceId).isEmpty()) {
            throw new CommonException("流程已没有待办任务，不能撤销");
        }
        Instance result = FlowEngine.taskService().revoke(instanceId, applicantActionParams("申请人撤销申请"));
        return result.getId();
    }

    private Instance lockOwnedInstance(Long instanceId) {
        if (instanceId == null || instanceId <= 0) {
            throw new CommonException("流程实例ID必须为正数");
        }
        if (!Integer.valueOf(java.sql.Connection.TRANSACTION_READ_COMMITTED)
                .equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())) {
            throw new CommonException("流程操作必须在READ_COMMITTED事务中执行，请检查外层事务配置");
        }
        String userId = identityProvider.currentUserId();
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null || inboxMapper.lockOwnedInstance(instanceId, tenantId.toString(), userId) == null) {
            throw new CommonException("流程实例不存在、已变化或无权操作");
        }
        Instance instance = FlowEngine.insService().getById(instanceId);
        if (instance == null || !tenantId.toString().equals(instance.getTenantId())
                || !userId.equals(instance.getCreateBy())) {
            throw new CommonException("流程实例不存在、已变化或无权操作");
        }
        return instance;
    }

    private void requireActiveInstance(Instance instance, List<FlowStatus> allowedStatuses) {
        if (!ActivityStatus.ACTIVITY.getKey().equals(instance.getActivityStatus())
                || allowedStatuses.stream().noneMatch(status -> status.getKey().equals(instance.getFlowStatus()))) {
            throw new CommonException("流程当前状态不支持此操作");
        }
    }

    private FlowParams applicantActionParams(String message) {
        List<String> flags = identityProvider.currentPermissionFlags();
        return FlowParams.build().handler(identityProvider.currentUserId())
                .permissionFlag(flags == null ? List.of() : flags).message(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Long submitBusinessTask(Long taskId, String businessType, String message, Map<String, Object> variables) {
        requireTaskId(taskId);
        requireText(businessType, "业务类型");
        Task task = lockAndAuthorize(taskId);
        var reference = WorkflowBusinessTasks.submission(businessProviders, task.getInstanceId(), task.getNodeCode());
        if (reference == null || !businessType.equals(reference.type())) {
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
