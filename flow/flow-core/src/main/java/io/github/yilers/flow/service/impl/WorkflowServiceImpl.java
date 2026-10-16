package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.service.WorkflowService;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Warm-Flow 1.8.9 适配实现。使用引擎的 pass/reject API，办理人权限由
 * {@link io.github.yilers.flow.config.FlowPermissionConfig} 统一提供。
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowIdentityProvider identityProvider;

    public WorkflowServiceImpl(WorkflowIdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(String flowCode, String businessId, Map<String, Object> variables) {
        requireText(flowCode, "流程编码");
        requireText(businessId, "业务单据ID");
        identityProvider.currentUserId();
        FlowParams params = FlowParams.build().flowCode(flowCode).variable(variablesOrEmpty(variables));
        Instance instance = FlowEngine.insService().start(businessId, params);
        return instance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long pass(Long taskId, String message, Map<String, Object> variables) {
        requireTaskId(taskId);
        identityProvider.currentUserId();
        return FlowEngine.taskService().pass(taskId, message, variablesOrEmpty(variables)).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reject(Long taskId, String message, Map<String, Object> variables) {
        requireTaskId(taskId);
        identityProvider.currentUserId();
        return FlowEngine.taskService().reject(taskId, message, variablesOrEmpty(variables)).getId();
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

    private static Map<String, Object> variablesOrEmpty(Map<String, Object> variables) {
        return variables == null ? Map.of() : variables;
    }
}
