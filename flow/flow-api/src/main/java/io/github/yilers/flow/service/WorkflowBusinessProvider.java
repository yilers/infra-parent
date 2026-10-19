package io.github.yilers.flow.service;

import io.github.yilers.flow.model.WorkflowBusinessReference;

/**
 * 业务模块提供自己的实例关联，不要求通用审批中心了解具体业务表。
 * 调用前已校验实例参与身份；正文接口仍须另行校验业务访问权限。
 */
public interface WorkflowBusinessProvider {

    /** 返回当前租户的关联；实例不属于本业务时返回null。 */
    WorkflowBusinessReference findReference(Long instanceId);
}
