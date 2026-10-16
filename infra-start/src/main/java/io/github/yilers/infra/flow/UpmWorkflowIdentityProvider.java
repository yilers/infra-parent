package io.github.yilers.infra.flow;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
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

    public UpmWorkflowIdentityProvider(UserService userService, UserRoleService userRoleService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
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
