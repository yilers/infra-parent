package io.github.yilers.flow.service;

import io.github.yilers.flow.model.WorkflowInstanceDetail;
import io.github.yilers.flow.model.WorkflowPage;

/**
 * 面向当前登录人的通用审批中心。身份和租户均从服务端上下文获取。
 * 菜单权限由调用入口校验，本服务始终限制数据可见范围，不提供管理员旁路。
 */
public interface WorkflowInboxService {

    /** 查询本人当前可办理的任务，流程编码可选且精确匹配。 */
    WorkflowPage todo(String flowCode, int pageNum, int pageSize);

    /** 查询本人实际办理过的审批节点记录，不将发起记录作为已办。 */
    WorkflowPage done(String flowCode, int pageNum, int pageSize);

    /** 查询本人发起的流程实例。 */
    WorkflowPage started(String flowCode, int pageNum, int pageSize);

    /** 查询本人收到的节点抄送，按生成时固化的接收用户查询。 */
    WorkflowPage cc(String flowCode, int pageNum, int pageSize);

    /** 仅接收人可以标记自己收到的抄送为已读，重复调用保持幂等。 */
    void readCc(Long ccId);

    /** 仅发起人、历史办理人、当前待办候选人或抄送接收人可查看实例详情。 */
    WorkflowInstanceDetail detail(Long instanceId);

    /** 校验当前人能否访问实例，供业务详情入口复用；不读取正文或流程变量。 */
    void checkInstanceAccess(Long instanceId);
}
