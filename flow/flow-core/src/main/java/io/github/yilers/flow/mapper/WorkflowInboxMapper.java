package io.github.yilers.flow.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.flow.model.WorkflowWorkItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审批中心只读查询。SQL 对每张表显式绑定可信租户，并保留租户插件的二次隔离。
 * 使用 EXISTS 避免用户同时命中角色、部门、用户权限时产生重复任务。
 */
public interface WorkflowInboxMapper {

    Page<WorkflowWorkItem> todo(Page<WorkflowWorkItem> page, @Param("tenantId") String tenantId,
                                @Param("userId") String userId, @Param("flags") List<String> flags,
                                @Param("flowCode") String flowCode);

    Page<WorkflowWorkItem> done(Page<WorkflowWorkItem> page, @Param("tenantId") String tenantId,
                                @Param("userId") String userId, @Param("flowCode") String flowCode);

    Page<WorkflowWorkItem> started(Page<WorkflowWorkItem> page, @Param("tenantId") String tenantId,
                                   @Param("userId") String userId, @Param("flowCode") String flowCode);

    Page<WorkflowWorkItem> cc(Page<WorkflowWorkItem> page, @Param("tenantId") String tenantId,
                              @Param("userId") String userId, @Param("flowCode") String flowCode);

    WorkflowWorkItem visibleInstance(@Param("instanceId") Long instanceId, @Param("tenantId") String tenantId,
                                     @Param("userId") String userId, @Param("flags") List<String> flags);

    List<WorkflowWorkItem> tasks(@Param("instanceId") Long instanceId, @Param("tenantId") String tenantId,
                                 @Param("userId") String userId, @Param("flags") List<String> flags);

    List<WorkflowWorkItem> history(@Param("instanceId") Long instanceId, @Param("tenantId") String tenantId);

    String chart(@Param("instanceId") Long instanceId, @Param("tenantId") String tenantId);

    /** 锁定任务所在实例，串行化同一实例的或签、会签及并行分支办理。 */
    Long lockInstanceByTask(@Param("taskId") Long taskId, @Param("tenantId") String tenantId);

    /** 锁定本人发起的流程实例，串行化拿回、撤销与审批办理。 */
    Long lockOwnedInstance(@Param("instanceId") Long instanceId, @Param("tenantId") String tenantId,
                           @Param("userId") String userId);

    /** 加锁后重新校验当前任务及候选身份，不能信任页面的actionable字段。 */
    int actionableTaskCount(@Param("taskId") Long taskId, @Param("tenantId") String tenantId,
                            @Param("userId") String userId, @Param("flags") List<String> flags);
}
