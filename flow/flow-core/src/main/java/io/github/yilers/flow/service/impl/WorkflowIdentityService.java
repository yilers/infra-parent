package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.model.WorkflowDepartmentInfo;
import io.github.yilers.flow.model.WorkflowLeaderType;
import io.github.yilers.flow.model.WorkflowRoleInfo;
import io.github.yilers.flow.model.WorkflowUserInfo;
import io.github.yilers.flow.model.WorkflowUserProfile;
import io.github.yilers.flow.service.WorkflowDataProvider;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.i18n.FlowMessage;
import io.github.yilers.web.exception.CommonException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flow 层身份与办理人规则服务。
 * <p>
 * 本类负责把宿主提供的基础用户、角色、部门和主管查询结果组装成流程办理人、抄送人
 * 及展示信息；不依赖 UPM 实体或具体数据访问 Service。
 */
@Service
public class WorkflowIdentityService implements WorkflowIdentityProvider {

    private final WorkflowDataProvider dataProvider;

    public WorkflowIdentityService(WorkflowDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public String currentUserId() {
        return requireCurrentUser().id();
    }

    @Override
    public String currentUserName() {
        WorkflowUserProfile user = requireCurrentUser();
        return displayName(user);
    }

    @Override
    public List<String> currentPermissionFlags() {
        WorkflowUserProfile user = requireCurrentUser();
        Set<String> flags = new LinkedHashSet<>();
        // Warm-Flow 默认匹配裸用户ID；带前缀标识用于设计器中明确区分用户、部门和角色。
        flags.add(user.id());
        flags.add("user:" + user.id());
        if (hasText(user.departmentId())) {
            flags.add("dept:" + user.departmentId());
        }
        dataProvider.findRoleIdsByUserId(user.id()).stream().filter(WorkflowIdentityService::hasText)
                .map(roleId -> "role:" + roleId).forEach(flags::add);
        return List.copyOf(flags);
    }

    @Override
    public Map<String, String> resolvePermissionNames(List<String> permissionFlags) {
        requireCurrentUser();
        Set<String> flags = permissionFlags == null ? Set.of() : new LinkedHashSet<>(permissionFlags);
        Set<String> userIds = new LinkedHashSet<>();
        Set<String> roleIds = new LinkedHashSet<>();
        Set<String> departmentIds = new LinkedHashSet<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (String flag : flags) {
            WorkflowAssigneeRule rule = WorkflowAssigneeRule.fromExpression(flag);
            if (rule != null) {
                names.put(flag, rule.getLabel());
                continue;
            }
            ParsedFlag parsed = parseFlag(flag, false);
            if (parsed == null) continue;
            switch (parsed.type()) {
                case "user" -> userIds.add(parsed.id());
                case "role" -> roleIds.add(parsed.id());
                case "dept" -> departmentIds.add(parsed.id());
                default -> { }
            }
        }
        dataProvider.findUsersByIds(List.copyOf(userIds)).forEach((id, user) -> names.put("user:" + id, displayName(user)));
        dataProvider.findRolesByIds(List.copyOf(roleIds)).forEach((id, role) -> names.put("role:" + id, role.name()));
        dataProvider.findDepartmentsByIds(List.copyOf(departmentIds))
                .forEach((id, department) -> names.put("dept:" + id, department.name()));
        // 兼容历史流程中未带 user: 前缀的用户办理人标识。
        userIds.forEach(id -> {
            String name = names.get("user:" + id);
            if (name != null) names.put(id, name);
        });
        return names;
    }

    @Override
    public List<String> resolveDynamicAssignees(String applicantId, WorkflowAssigneeRule rule) {
        WorkflowUserProfile currentUser = requireCurrentUser();
        if (!currentUser.id().equals(applicantId)) {
            throw new CommonException(FlowMessage.IDENTITY_APPLICANT_ONLY);
        }
        WorkflowUserProfile applicant = dataProvider.findUsersByIds(List.of(applicantId)).get(applicantId);
        if (applicant == null) {
            throw new CommonException(FlowMessage.IDENTITY_APPLICANT_MISSING);
        }
        return resolveApplicantRule(applicant, rule);
    }

    @Override
    public List<String> resolveUserIds(List<String> permissionFlags) {
        requireCurrentUser();
        return resolveUserIdsFromFlags(permissionFlags);
    }

    @Override
    public List<String> resolveCcUserIds(String applicantId, List<String> recipients, boolean includeChildDepartments) {
        requireCurrentUser();
        WorkflowUserProfile applicant = dataProvider.findUsersByIds(List.of(applicantId)).get(applicantId);
        LinkedHashSet<String> users = new LinkedHashSet<>();
        if (recipients == null || recipients.isEmpty()) return List.of();

        List<String> staticFlags = new ArrayList<>();
        for (String flag : recipients) {
            WorkflowAssigneeRule rule = WorkflowAssigneeRule.fromExpression(flag);
            if (rule == null) {
                staticFlags.add(flag);
                continue;
            }
            if (applicant != null) {
                try {
                    users.addAll(resolveApplicantRule(applicant, rule));
                } catch (CommonException ignored) {
                    // 组织关系变化时跳过无法解析的动态抄送人，不回滚已经完成的审批。
                }
            }
        }

        users.addAll(resolveCcStaticRecipients(staticFlags, includeChildDepartments));
        return List.copyOf(users);
    }

    @Override
    public Map<String, WorkflowUserInfo> resolveUsers(List<String> userIds) {
        requireCurrentUser();
        if (userIds == null || userIds.isEmpty()) return Map.of();
        List<String> ids = userIds.stream().filter(WorkflowIdentityService::isNumericId).distinct().toList();
        Map<String, WorkflowUserInfo> result = new LinkedHashMap<>();
        dataProvider.findUsersByIds(ids).forEach((id, user) -> result.put(id, new WorkflowUserInfo(id, displayName(user))));
        return result;
    }

    private List<String> resolveUserIdsFromFlags(List<String> permissionFlags) {
        if (permissionFlags == null || permissionFlags.isEmpty()) return List.of();
        Set<String> userIds = new LinkedHashSet<>();
        Set<String> roleIds = new LinkedHashSet<>();
        Set<String> departmentIds = new LinkedHashSet<>();
        for (String flag : permissionFlags) {
            ParsedFlag parsed = parseFlag(flag, true);
            switch (parsed.type()) {
                case "user" -> userIds.add(parsed.id());
                case "role" -> roleIds.add(parsed.id());
                case "dept" -> departmentIds.add(parsed.id());
                default -> throw new IllegalArgumentException("不支持的流程办理人类型");
            }
        }
        Map<String, WorkflowRoleInfo> roles = dataProvider.findRolesByIds(List.copyOf(roleIds));
        if (!roles.keySet().containsAll(roleIds)) {
            throw new IllegalArgumentException("流程办理角色不存在或已停用");
        }
        Map<String, WorkflowDepartmentInfo> departments = dataProvider.findDepartmentsByIds(List.copyOf(departmentIds));
        if (!departments.keySet().containsAll(departmentIds)) {
            throw new IllegalArgumentException("流程办理部门不存在或已停用");
        }
        userIds.addAll(dataProvider.findUserIdsByRoleIds(List.copyOf(roleIds)));
        userIds.addAll(dataProvider.findUserIdsByDepartmentIds(List.copyOf(departmentIds)));
        if (userIds.isEmpty()) return List.of();
        Map<String, WorkflowUserProfile> users = dataProvider.findUsersByIds(List.copyOf(userIds));
        return userIds.stream().filter(users::containsKey).toList();
    }

    private List<String> resolveCcStaticRecipients(List<String> flags, boolean includeChildDepartments) {
        Set<String> userIds = new LinkedHashSet<>();
        Set<String> roleIds = new LinkedHashSet<>();
        Set<String> departmentIds = new LinkedHashSet<>();
        for (String flag : flags) {
            ParsedFlag parsed = parseFlag(flag, false);
            if (parsed == null) continue;
            switch (parsed.type()) {
                case "user" -> userIds.add(parsed.id());
                case "role" -> roleIds.add(parsed.id());
                case "dept" -> departmentIds.add(parsed.id());
                default -> { }
            }
        }

        Map<String, WorkflowRoleInfo> roles = dataProvider.findRolesByIds(List.copyOf(roleIds));
        roleIds.retainAll(roles.keySet());
        Map<String, WorkflowDepartmentInfo> departments = dataProvider.findDepartmentsByIds(List.copyOf(departmentIds));
        departmentIds.retainAll(departments.keySet());
        if (includeChildDepartments && !departmentIds.isEmpty()) {
            departmentIds = expandChildDepartments(departmentIds);
        }
        userIds.addAll(dataProvider.findUserIdsByRoleIds(List.copyOf(roleIds)));
        userIds.addAll(dataProvider.findUserIdsByDepartmentIds(List.copyOf(departmentIds)));
        return List.copyOf(dataProvider.findUsersByIds(List.copyOf(userIds)).keySet());
    }

    private Set<String> expandChildDepartments(Set<String> roots) {
        List<WorkflowDepartmentInfo> all = dataProvider.findAllDepartments();
        Map<String, List<String>> children = new LinkedHashMap<>();
        Set<String> knownDepartments = new LinkedHashSet<>();
        for (WorkflowDepartmentInfo department : all) {
            knownDepartments.add(department.id());
            if (hasText(department.parentId())) {
                children.computeIfAbsent(department.parentId(), ignored -> new ArrayList<>()).add(department.id());
            }
        }
        Set<String> result = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>(roots.stream().filter(knownDepartments::contains).toList());
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (result.add(current)) queue.addAll(children.getOrDefault(current, List.of()));
        }
        return result;
    }

    private List<String> resolveApplicantRule(WorkflowUserProfile applicant, WorkflowAssigneeRule rule) {
        if (rule == WorkflowAssigneeRule.APPLICANT) return List.of(applicant.id());
        if (!hasText(applicant.departmentId())) {
            throw new CommonException(FlowMessage.IDENTITY_DEPT_REQUIRED);
        }
        WorkflowDepartmentInfo department = dataProvider.findDepartmentsByIds(List.of(applicant.departmentId()))
                .get(applicant.departmentId());
        requireDepartment(department, FlowMessage.IDENTITY_DEPT_MISSING);

        if (rule == WorkflowAssigneeRule.PARENT_DEPARTMENT_APPROVERS) {
            if (!hasText(department.parentId())) {
                throw new CommonException(FlowMessage.IDENTITY_PARENT_REQUIRED);
            }
            WorkflowDepartmentInfo parent = dataProvider.findDepartmentsByIds(List.of(department.parentId()))
                    .get(department.parentId());
            requireDepartment(parent, FlowMessage.IDENTITY_PARENT_REQUIRED);
            List<String> leaders = activeLeaders(parent.id(), WorkflowLeaderType.PRIMARY, applicant.id());
            if (leaders.isEmpty()) {
                throw new CommonException(FlowMessage.IDENTITY_PARENT_LEADER_MISSING);
            }
            return leaders;
        }

        List<String> deputies = activeLeaders(department.id(), WorkflowLeaderType.DEPUTY, applicant.id());
        if (!deputies.isEmpty()) return deputies;
        List<String> leaders = activeLeaders(department.id(), WorkflowLeaderType.PRIMARY, applicant.id());
        if (leaders.isEmpty()) {
            throw new CommonException(FlowMessage.IDENTITY_DEPT_LEADER_MISSING);
        }
        return leaders;
    }

    private List<String> activeLeaders(String departmentId, WorkflowLeaderType type, String applicantId) {
        List<String> ids = dataProvider.findDepartmentLeaderUserIds(departmentId, type).stream()
                .filter(WorkflowIdentityService::isNumericId).filter(id -> !id.equals(applicantId)).distinct().toList();
        if (ids.isEmpty()) return List.of();
        return dataProvider.findUsersByIds(ids).keySet().stream().filter(id -> !id.equals(applicantId)).toList();
    }

    private void requireDepartment(WorkflowDepartmentInfo department, FlowMessage message) {
        if (department == null) throw new CommonException(message);
    }

    private WorkflowUserProfile requireCurrentUser() {
        WorkflowUserProfile user = dataProvider.currentUser();
        if (user == null || !isNumericId(user.id())) {
            throw new CommonException(FlowMessage.IDENTITY_USER_INVALID);
        }
        return user;
    }

    private static ParsedFlag parseFlag(String flag, boolean strict) {
        if (!hasText(flag)) {
            if (strict) throw new IllegalArgumentException("流程办理人标识不能为空");
            return null;
        }
        String[] parts = flag.split(":", 2);
        String type = parts.length == 1 ? "user" : parts[0];
        String id = parts.length == 1 ? parts[0] : parts[1];
        if (!isNumericId(id)) {
            if (strict) throw new IllegalArgumentException("流程办理人标识不合法");
            return null;
        }
        return new ParsedFlag(type, id);
    }

    private static boolean isNumericId(String id) {
        if (id == null || !id.matches("[0-9]{1,19}")) return false;
        try {
            return Long.parseLong(id) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String displayName(WorkflowUserProfile user) {
        return hasText(user.name()) ? user.name() : user.id();
    }

    private record ParsedFlag(String type, String id) {
    }
}
