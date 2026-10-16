package io.github.yilers.flow.service;

import java.util.Map;

/**
 * 业务模块调用流程引擎的最小契约，不向业务模块暴露 Warm-Flow 类型。
 * <p>
 * 调用方必须先校验对业务单据的访问和操作权限；本服务另外校验当前登录身份、
 * 流程租户及任务办理人权限。流程与业务单据的事务应在同一数据源中完成。
 */
public interface WorkflowService {

    /**
     * 按已发布的流程编码发起业务流程。
     *
     * @param flowCode 已发布流程编码
     * @param businessId 业务单据ID，建议由业务模块提供不可变的唯一值
     * @param variables 流程路由变量，不应放入敏感业务正文
     * @return 流程实例ID
     */
    Long start(String flowCode, String businessId, Map<String, Object> variables);

    /**
     * 审批通过当前待办任务。
     *
     * @return 流程实例ID
     */
    Long pass(Long taskId, String message, Map<String, Object> variables);

    /**
     * 驳回当前待办任务。
     *
     * @return 流程实例ID
     */
    Long reject(Long taskId, String message, Map<String, Object> variables);
}
