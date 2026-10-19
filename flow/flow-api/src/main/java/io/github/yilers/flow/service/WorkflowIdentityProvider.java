package io.github.yilers.flow.service;

import java.util.List;

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

    /**
     * 当前用户能够匹配的流程办理人标识，例如用户ID、role:角色ID和dept:部门ID。
     */
    List<String> currentPermissionFlags();

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
