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
     * 业务Service完成申请人的表单提交任务；必须先校验、保存业务单据，再在同一事务内调用。
     * 只允许已被业务关联识别的提交任务，不对浏览器开放通用HTTP入口。
     */
    Long submitBusinessTask(Long taskId, String businessType, String message, Map<String, Object> variables);

    /**
     * 驳回当前待办任务。
     *
     * @return 流程实例ID
     */
    Long reject(Long taskId, String message, Map<String, Object> variables);

    /** 将本人当前待办转交给同租户的另一名有效用户。 */
    Long transfer(Long taskId, Long targetUserId, String message);

    /** 委派本人当前待办；受托人办理后任务按 Warm-Flow 规则回到委派人。 */
    Long delegate(Long taskId, Long targetUserId, String message);

    /** 申请人拿回仍在审批中的实例，退回到自己最近办理过的任务。 */
    Long takeBack(Long instanceId);

    /** 申请人撤销尚未办结的实例，流程不再继续流转。 */
    Long revoke(Long instanceId);
}
