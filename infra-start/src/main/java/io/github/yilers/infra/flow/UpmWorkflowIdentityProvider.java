package io.github.yilers.infra.flow;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 单体启动时的 UPM 工作流身份适配器。
 * <p>
 * 流程任务权限使用实际用户、角色和部门ID；不使用菜单按钮权限替代办理人权限。
 * 当前租户中的用户不存在或被停用时，拒绝继续办理流程。
 */
@Component
public class UpmWorkflowIdentityProvider implements WorkflowIdentityProvider {

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final DeptService deptService;

    public UpmWorkflowIdentityProvider(UserService userService, UserRoleService userRoleService,
                                       RoleService roleService, DeptService deptService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.roleService = roleService;
        this.deptService = deptService;
    }

    @Override
    public String currentUserId() {
        return currentUser().getId().toString();
    }

    @Override
    public List<String> currentPermissionFlags() {
        User user = currentUser();
        Set<String> flags = new LinkedHashSet<>();
        // Warm-Flow 默认以用户ID作为办理人标识；同时保留显式 user: 前缀供流程设计器使用。
        flags.add(user.getId().toString());
        flags.add("user:" + user.getId());
        if (user.getDeptId() != null) {
            flags.add("dept:" + user.getDeptId());
        }
        for (Role role : userRoleService.findRoleListByUserId(user.getId())) {
            flags.add("role:" + role.getId());
        }
        return new ArrayList<>(flags);
    }

    /**
     * 在任务创建时将角色、部门展开为本租户有效用户。
     * 部门表示该部门员工，不等同于主管；主管规则由业务另外解析。
     */
    @Override
    public List<String> resolveUserIds(List<String> permissionFlags) {
        currentUser();
        Long tenantId = RequestContextHolder.getTenantId();
        Set<Long> userIds = new LinkedHashSet<>();
        for (String flag : permissionFlags) {
            String[] parts = flag.split(":", 2);
            String type = parts.length == 1 ? "user" : parts[0];
            String value = parts.length == 1 ? parts[0] : parts[1];
            if (!value.matches("[0-9]+")) {
                throw new IllegalArgumentException("流程办理人标识不合法");
            }
            Long id = Long.valueOf(value);
            switch (type) {
                case "user" -> userIds.add(id);
                case "role" -> {
                    Role role = roleService.getById(id);
                    if (role == null || !tenantId.equals(role.getTenantId()) || !Integer.valueOf(1).equals(role.getUsable())) {
                        throw new IllegalArgumentException("流程办理角色不存在或已停用");
                    }
                    userIds.addAll(userRoleService.findUserIdListByRoleId(id));
                }
                case "dept" -> {
                    Dept dept = deptService.getById(id);
                    if (dept == null || !tenantId.equals(dept.getTenantId()) || !Integer.valueOf(1).equals(dept.getUsable())) {
                        throw new IllegalArgumentException("流程办理部门不存在或已停用");
                    }
                    userService.lambdaQuery().eq(User::getDeptId, id)
                            .eq(User::getTenantId, tenantId).eq(User::getUsable, 1).list()
                            .forEach(user -> userIds.add(user.getId()));
                }
                default -> throw new IllegalArgumentException("不支持的流程办理人类型");
            }
        }
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userService.listByIds(userIds).stream()
                .filter(user -> tenantId.equals(user.getTenantId()) && Integer.valueOf(1).equals(user.getUsable()))
                .map(user -> user.getId().toString()).distinct().toList();
    }

    private User currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Flow 租户上下文不能为空");
        }
        User user = userService.getById(userId);
        if (user == null || !tenantId.equals(user.getTenantId()) || !Integer.valueOf(1).equals(user.getUsable())) {
            throw new IllegalStateException("当前用户不属于可用的流程租户");
        }
        return user;
    }
}
