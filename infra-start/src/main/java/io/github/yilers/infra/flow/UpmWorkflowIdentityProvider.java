package io.github.yilers.infra.flow;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.DeptLeader;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.DeptLeaderService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final DeptLeaderService deptLeaderService;

    public UpmWorkflowIdentityProvider(UserService userService, UserRoleService userRoleService,
                                       RoleService roleService, DeptService deptService, DeptLeaderService deptLeaderService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.roleService = roleService;
        this.deptService = deptService;
        this.deptLeaderService = deptLeaderService;
    }

    @Override
    public String currentUserId() {
        return currentUser().getId().toString();
    }

    @Override
    public String currentUserName() {
        User user = currentUser();
        return user.getName() == null || user.getName().isBlank() ? user.getAccount() : user.getName();
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

    /** 仅解析当前租户的展示名称；历史流程中的已删除或无效标识由流程图明确提示。 */
    @Override
    public Map<String, String> resolvePermissionNames(List<String> permissionFlags) {
        currentUser();
        Long tenantId = RequestContextHolder.getTenantId();
        Map<String, String> names = new LinkedHashMap<>();
        for (String flag : new LinkedHashSet<>(permissionFlags)) {
            WorkflowAssigneeRule rule = WorkflowAssigneeRule.fromExpression(flag);
            if (rule != null) {
                names.put(flag, rule.getLabel());
                continue;
            }
            String[] parts = flag.split(":", 2);
            String type = parts.length == 1 ? "user" : parts[0];
            String value = parts.length == 1 ? parts[0] : parts[1];
            if (!value.matches("[0-9]{1,19}")) {
                continue;
            }
            Long id;
            try {
                id = Long.valueOf(value);
            } catch (NumberFormatException ignored) {
                continue;
            }
            String name = switch (type) {
                case "role" -> {
                    Role role = roleService.getById(id);
                    yield role != null && tenantId.equals(role.getTenantId()) ? role.getRoleName() : null;
                }
                case "dept" -> {
                    Dept dept = deptService.getById(id);
                    yield dept != null && tenantId.equals(dept.getTenantId()) ? dept.getDeptName() : null;
                }
                case "user" -> {
                    User user = userService.getById(id);
                    yield user != null && tenantId.equals(user.getTenantId())
                            ? (user.getName() == null || user.getName().isBlank() ? user.getAccount() : user.getName()) : null;
                }
                default -> null;
            };
            if (name != null && !name.isBlank()) {
                names.put(flag, name);
            }
        }
        return names;
    }

    /** 申请人的组织关系取自可信登录身份；部门与主管记录均再次校验租户。 */
    @Override
    public List<String> resolveDynamicAssignees(String applicantId, WorkflowAssigneeRule rule) {
        User applicant = currentUser();
        if (!applicant.getId().toString().equals(applicantId)) {
            throw new CommonException("动态办理人只能依据当前申请人的组织信息生成");
        }
        if (rule == WorkflowAssigneeRule.APPLICANT) {
            return List.of(applicantId);
        }
        Long tenantId = RequestContextHolder.getTenantId();
        Dept department = applicant.getDeptId() == null ? null : deptService.getById(applicant.getDeptId());
        requireActiveDepartment(department, tenantId, "申请人所在部门不存在或已停用");
        if (rule == WorkflowAssigneeRule.PARENT_DEPARTMENT_APPROVERS) {
            Dept parent = department.getParentId() == null ? null : deptService.getById(department.getParentId());
            requireActiveDepartment(parent, tenantId, "申请人所在部门没有有效的上一级部门");
            List<String> leaders = activeLeaders(parent.getId(), 1, applicant.getId(), tenantId);
            if (leaders.isEmpty()) {
                throw new CommonException("上一级部门未配置有效主管，或主管就是申请人本人");
            }
            return leaders;
        }
        List<String> deputies = activeLeaders(department.getId(), 2, applicant.getId(), tenantId);
        if (!deputies.isEmpty()) {
            return deputies;
        }
        List<String> leaders = activeLeaders(department.getId(), 1, applicant.getId(), tenantId);
        if (leaders.isEmpty()) {
            throw new CommonException("本部门未配置有效副主管或主管，或审批人就是申请人本人");
        }
        return leaders;
    }

    private void requireActiveDepartment(Dept dept, Long tenantId, String message) {
        if (dept == null || !tenantId.equals(dept.getTenantId()) || !Integer.valueOf(1).equals(dept.getUsable())) {
            throw new CommonException(message);
        }
    }

    /** 任职有效期由组织Service筛选，这里过滤本人、跨租户、已停用或已删除的账号。 */
    private List<String> activeLeaders(Long deptId, int type, Long applicantId, Long tenantId) {
        List<Long> ids = deptLeaderService.findCurrentByDeptId(deptId).stream()
                .filter(leader -> tenantId.equals(leader.getTenantId()) && deptId.equals(leader.getDeptId())
                        && Integer.valueOf(type).equals(leader.getLeaderType()) && leader.getUserId() != null
                        && !applicantId.equals(leader.getUserId()))
                .map(DeptLeader::getUserId).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return userService.listByIds(ids).stream()
                .filter(user -> tenantId.equals(user.getTenantId()) && Integer.valueOf(1).equals(user.getUsable())
                        && ids.contains(user.getId()))
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
