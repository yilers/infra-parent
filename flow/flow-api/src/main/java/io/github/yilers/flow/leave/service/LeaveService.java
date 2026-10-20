package io.github.yilers.flow.leave.service;

import io.github.yilers.flow.leave.model.LeaveInfo;
import io.github.yilers.flow.leave.model.LeavePage;
import io.github.yilers.flow.leave.request.LeaveSaveRequest;

/** 请假示例业务契约，不把请假字段加入通用WorkflowService。 */
public interface LeaveService {

    /** 仅查询当前租户本人单据；状态为空时不筛选。 */
    LeavePage page(String status, int pageNum, int pageSize);

    /** 新建本人草稿，不启动流程。 */
    LeaveInfo create(LeaveSaveRequest request);

    /** 本人草稿或当前申请提交任务可修改，校验任务与版本，审批中不可修改。 */
    LeaveInfo edit(Long id, LeaveSaveRequest request);

    /** 首次提交自动完成申请节点；重新提交继续原实例，校验当前任务与单据版本。 */
    Long submit(Long id, Integer version, Long submissionTaskId);

    /** 兼容已有草稿提交调用。已发起的申请必须使用带当前任务ID的重载。 */
    Long submit(Long id, Integer version);

    /** 仅物理删除本人草稿，不允许删除已有审批实例的单据。 */
    void deleteDraft(Long id, Integer version);

    /** 本人可看草稿；已提交单据还允许该实例的当前候选人或历史办理人查看。 */
    LeaveInfo detail(Long id);
}
