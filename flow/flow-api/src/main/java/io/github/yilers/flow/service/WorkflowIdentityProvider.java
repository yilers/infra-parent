package io.github.yilers.flow.service;

import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.model.WorkflowUserInfo;

import java.util.List;
import java.util.Map;

/**
 * Flow 组装后的登录身份、办理人和展示信息能力，由 flow-core 实现。
 * <p>
 * 宿主只需实现 {@link WorkflowDataProvider} 基础数据查询端口；Flow 根据这些数据
 * 解析办理人、动态规则和抄送对象。返回的权限标识只用于任务办理人匹配，
 * 不替代接口上的菜单按钮鉴权。
 */
public interface WorkflowIdentityProvider {

    /** 当前已登录用户的唯一标识；未登录时实现必须拒绝访问。 */
    String currentUserId();

    /** 当前人姓名，仅用于业务单据的展示快照。 */
    String currentUserName();

    /** 当前用户能够匹配的流程办理人标识，例如用户ID、role:角色ID和dept:部门ID。 */
    List<String> currentPermissionFlags();

    /** 解析当前租户中办理人标识的展示名称，供已授权的实例流程图使用。 */
    Map<String, String> resolvePermissionNames(List<String> permissionFlags);

    /** 按可信申请人及其组织关系解析动态办理规则，发起时固化为实例变量。 */
    List<String> resolveDynamicAssignees(String applicantId, WorkflowAssigneeRule rule);

    /** 将设计器办理人标识解析为有效用户ID，供引擎创建任务时固化候选人。 */
    List<String> resolveUserIds(List<String> permissionFlags);

    /** 节点通过时按申请人及组织关系解析同租户有效抄送用户。 */
    List<String> resolveCcUserIds(String applicantId, List<String> recipients, boolean includeChildDepartments);

    /** 按用户ID批量读取当前租户的用户展示信息，历史记录与抄送共用。 */
    Map<String, WorkflowUserInfo> resolveUsers(List<String> userIds);
}
