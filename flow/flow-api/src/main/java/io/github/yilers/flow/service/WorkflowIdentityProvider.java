package io.github.yilers.flow.service;

import io.github.yilers.flow.model.WorkflowAssigneeRule;

import java.util.List;
import java.util.Map;

/**
 * 工作流所需的登录身份能力。
 * <p>
 * flow-api 不依赖 UPM 的实体和 Service。单体部署由组装层实现，独立部署时
 * 可以通过 RPC 或 HTTP 实现同一契约。返回的权限标识只用于任务办理人匹配，
 * 不替代接口上的菜单按钮鉴权。
 */
public interface WorkflowIdentityProvider {

    /**
     * 当前已登录用户的唯一标识。未登录时应拒绝访问，不得返回匿名用户。
     */
    String currentUserId();

    /** 当前人姓名，仅用于业务单据的展示快照；宿主未提供时使用用户ID。 */
    default String currentUserName() {
        return currentUserId();
    }

    /**
     * 当前用户能够匹配的流程办理人标识，例如用户ID、role:角色ID和dept:部门ID。
     */
    List<String> currentPermissionFlags();

    /**
     * 解析当前租户中办理人标识的展示名称，供已授权的实例流程图使用。
     * 不展开候选账号、不授予目录查询权限；无法解析的标识不返回名称。
     */
    default Map<String, String> resolvePermissionNames(List<String> permissionFlags) {
        return Map.of();
    }

    /**
     * 按可信申请人解析规则对应的有效用户ID，宿主负责租户、组织和任职有效期校验。
     * 发起时解析并持久化为实例变量，不根据后续办理人的部门重新选人。
     */
    default List<String> resolveDynamicAssignees(String applicantId, WorkflowAssigneeRule rule) {
        if (rule == WorkflowAssigneeRule.APPLICANT) {
            return List.of(applicantId);
        }
        throw new IllegalStateException("宿主尚未实现动态办理人规则：" + rule.getLabel());
    }

    /**
     * 将设计器办理人标识解析为实际用户ID，供引擎创建任务时固化候选人。
     * 默认支持用户标识；角色、部门由宿主实现，不能让Flow直接依赖UPM。
     */
    default List<String> resolveUserIds(List<String> permissionFlags) {
        return permissionFlags.stream().map(flag -> {
            if (flag.startsWith("user:")) {
                return flag.substring(5);
            }
            if (flag.contains(":")) {
                throw new IllegalStateException("宿主尚未实现角色或部门办理人解析");
            }
            return flag;
        }).distinct().toList();
    }
}
