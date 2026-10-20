package io.github.yilers.flow.service;

import io.github.yilers.flow.model.WorkflowBusinessReference;

/**
 * 业务模块提供自己的实例关联，不要求通用审批中心了解具体业务表。
 * 调用前已校验实例参与身份；正文接口仍须另行校验业务访问权限。
 */
public interface WorkflowBusinessProvider {

    /** 返回当前租户的关联；实例不属于本业务时返回null。 */
    WorkflowBusinessReference findReference(Long instanceId);

    /**
     * 业务表单提交任务的可信关联；普通审批任务返回null。
     * 此类任务必须由业务Service保存、校验单据后完成，不能使用通用审批通过/退回接口。
     */
    default WorkflowBusinessReference findSubmissionTask(Long instanceId, String nodeCode) {
        return null;
    }
}
