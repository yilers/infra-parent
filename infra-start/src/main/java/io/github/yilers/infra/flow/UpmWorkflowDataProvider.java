package io.github.yilers.infra.flow;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.flow.model.WorkflowDepartmentInfo;
import io.github.yilers.flow.model.WorkflowDirectoryEntry;
import io.github.yilers.flow.model.WorkflowDirectoryPage;
import io.github.yilers.flow.model.WorkflowDirectoryType;
import io.github.yilers.flow.model.WorkflowLeaderType;
import io.github.yilers.flow.model.WorkflowRoleInfo;
import io.github.yilers.flow.model.WorkflowUserProfile;
import io.github.yilers.flow.service.WorkflowDataProvider;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.DeptLeader;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserRole;
import io.github.yilers.upm.enums.DeptLeaderTypeEnum;
import io.github.yilers.upm.service.DeptLeaderService;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 单体部署下的 UPM 基础数据查询实现。
 * <p>
 * 本类只负责读取并校验当前租户内的有效身份、用户和组织数据，不展开流程办理人规则，
 * 也不组装抄送、流程图或设计器展示结果。微服务部署时可由远程查询实现替换此类。
 */
@Component
public class UpmWorkflowDataProvider implements WorkflowDataProvider {

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final DeptService deptService;
    private final DeptLeaderService deptLeaderService;

    public UpmWorkflowDataProvider(UserService userService, UserRoleService userRoleService,
                                  RoleService roleService, DeptService deptService,
                                  DeptLeaderService deptLeaderService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.roleService = roleService;
        this.deptService = deptService;
        this.deptLeaderService = deptLeaderService;
    }

    @Override
    public WorkflowUserProfile currentUser() {
        Long tenantId = requireTenantId();
        User user = userService.getById(StpUtil.getLoginIdAsLong());
        if (!isActiveUser(user, tenantId)) {
            throw new IllegalStateException("当前用户不属于可用的流程租户");
        }
        return toProfile(user);
    }

    @Override
    public Map<String, WorkflowUserProfile> findUsersByIds(List<String> userIds) {
        Long tenantId = requireTenantId();
        List<Long> ids = parseIds(userIds);
        if (ids.isEmpty()) return Map.of();
        Map<String, WorkflowUserProfile> result = new LinkedHashMap<>();
        userService.listByIds(ids).stream().filter(user -> isActiveUser(user, tenantId))
                .forEach(user -> result.put(user.getId().toString(), toProfile(user)));
        return result;
    }

    @Override
    public Map<String, WorkflowRoleInfo> findRolesByIds(List<String> roleIds) {
        Long tenantId = requireTenantId();
        List<Long> ids = parseIds(roleIds);
        if (ids.isEmpty()) return Map.of();
        Map<String, WorkflowRoleInfo> result = new LinkedHashMap<>();
        roleService.listByIds(ids).stream().filter(role -> isActiveTenantData(role.getTenantId(), role.getUsable(), tenantId))
                .forEach(role -> result.put(role.getId().toString(),
                        new WorkflowRoleInfo(role.getId().toString(), role.getRoleName())));
        return result;
    }

    @Override
    public Map<String, WorkflowDepartmentInfo> findDepartmentsByIds(List<String> departmentIds) {
        Long tenantId = requireTenantId();
        List<Long> ids = parseIds(departmentIds);
        if (ids.isEmpty()) return Map.of();
        Map<String, WorkflowDepartmentInfo> result = new LinkedHashMap<>();
        deptService.listByIds(ids).stream()
                .filter(department -> isActiveTenantData(department.getTenantId(), department.getUsable(), tenantId))
                .forEach(department -> result.put(department.getId().toString(), toDepartmentInfo(department)));
        return result;
    }

    @Override
    public List<String> findUserIdsByRoleIds(List<String> roleIds) {
        Long tenantId = requireTenantId();
        List<Long> ids = parseIds(roleIds);
        if (ids.isEmpty()) return List.of();
        Map<String, WorkflowRoleInfo> activeRoles = findRolesByIds(roleIds);
        List<Long> activeRoleIds = activeRoles.keySet().stream().map(Long::valueOf).toList();
        if (activeRoleIds.isEmpty()) return List.of();
        List<Long> userIds = userRoleService.list(Wrappers.<UserRole>lambdaQuery()
                        .in(UserRole::getRoleId, activeRoleIds)
                        .eq(UserRole::getTenantId, tenantId)).stream()
                .map(UserRole::getUserId).filter(Objects::nonNull).distinct().toList();
        return findUsersByIds(toStringIds(userIds)).keySet().stream().toList();
    }

    @Override
    public List<String> findUserIdsByDepartmentIds(List<String> departmentIds) {
        Long tenantId = requireTenantId();
        List<Long> requestedIds = parseIds(departmentIds);
        if (requestedIds.isEmpty()) return List.of();
        List<Long> activeIds = findDepartmentsByIds(departmentIds).keySet().stream().map(Long::valueOf).toList();
        if (activeIds.isEmpty()) return List.of();
        List<String> userIds = userService.lambdaQuery().in(User::getDeptId, activeIds)
                .eq(User::getTenantId, tenantId).eq(User::getUsable, CommonConst.YES)
                .list().stream().map(user -> user.getId().toString()).toList();
        return userIds;
    }

    @Override
    public List<String> findRoleIdsByUserId(String userId) {
        Long tenantId = requireTenantId();
        Long id = parseId(userId);
        if (id == null) return List.of();
        List<String> roleIds = userRoleService.findRoleListByUserId(id).stream()
                .filter(role -> isActiveTenantData(role.getTenantId(), role.getUsable(), tenantId))
                .map(role -> role.getId().toString()).toList();
        return roleIds;
    }

    @Override
    public List<String> findDepartmentLeaderUserIds(String departmentId, WorkflowLeaderType leaderType) {
        Long tenantId = requireTenantId();
        Long deptId = parseId(departmentId);
        if (deptId == null || leaderType == null) return List.of();
        DeptLeaderTypeEnum upmType = switch (leaderType) {
            case PRIMARY -> DeptLeaderTypeEnum.PRIMARY;
            case DEPUTY -> DeptLeaderTypeEnum.DEPUTY;
        };
        List<String> candidateIds = deptLeaderService.findCurrentByDeptId(deptId).stream()
                .filter(leader -> Objects.equals(leader.getTenantId(), tenantId)
                        && Objects.equals(leader.getDeptId(), deptId)
                        && Objects.equals(leader.getLeaderType(), upmType.getCode())
                        && leader.getUserId() != null)
                .map(leader -> leader.getUserId().toString()).distinct().toList();
        return List.copyOf(findUsersByIds(candidateIds).keySet());
    }

    @Override
    public List<WorkflowDepartmentInfo> findAllDepartments() {
        Long tenantId = requireTenantId();
        return deptService.lambdaQuery().eq(Dept::getTenantId, tenantId)
                .eq(Dept::getUsable, CommonConst.YES).list().stream().map(UpmWorkflowDataProvider::toDepartmentInfo).toList();
    }

    @Override
    public WorkflowDirectoryPage searchDirectory(WorkflowDirectoryType type, String keyword, String departmentId,
                                                  long pageNum, long pageSize) {
        Long tenantId = requireTenantId();
        if (type == null || pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("流程目录查询参数不合法");
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        return switch (type) {
            case ROLE -> {
                Page<Role> page = roleService.lambdaQuery().eq(Role::getTenantId, tenantId)
                        .eq(Role::getUsable, CommonConst.YES)
                        .like(normalizedKeyword != null, Role::getRoleName, normalizedKeyword)
                        .page(new Page<>(pageNum, pageSize));
                yield toDirectoryPage(page, page.getRecords().stream().map(role -> new WorkflowDirectoryEntry(
                        role.getId().toString(), role.getRoleCode(), role.getRoleName(),
                        WorkflowDirectoryType.ROLE, role.getCreateTime())).toList());
            }
            case DEPARTMENT -> {
                Page<Dept> page = deptService.lambdaQuery().eq(Dept::getTenantId, tenantId)
                        .eq(Dept::getUsable, CommonConst.YES)
                        .like(normalizedKeyword != null, Dept::getDeptName, normalizedKeyword)
                        .page(new Page<>(pageNum, pageSize));
                yield toDirectoryPage(page, page.getRecords().stream().map(dept -> new WorkflowDirectoryEntry(
                        dept.getId().toString(), dept.getDeptCode(), dept.getDeptName(),
                        WorkflowDirectoryType.DEPARTMENT, dept.getCreateTime())).toList());
            }
            case USER -> {
                Long deptId = departmentId == null || departmentId.isBlank() ? null : parseId(departmentId);
                if (departmentId != null && !departmentId.isBlank() && deptId == null) {
                    yield new WorkflowDirectoryPage(pageNum, pageSize, 0, List.of());
                }
                Page<User> page = userService.lambdaQuery().eq(User::getTenantId, tenantId)
                        .eq(User::getUsable, CommonConst.YES)
                        .eq(deptId != null, User::getDeptId, deptId)
                        .and(normalizedKeyword != null, query -> query
                                .like(User::getName, normalizedKeyword).or().like(User::getAccount, normalizedKeyword))
                        .page(new Page<>(pageNum, pageSize));
                yield toDirectoryPage(page, page.getRecords().stream().map(user -> new WorkflowDirectoryEntry(
                        user.getId().toString(), user.getAccount(), displayName(user),
                        WorkflowDirectoryType.USER, user.getCreateTime())).toList());
            }
        };
    }

    @Override
    public WorkflowDirectoryPage searchTransferTargets(String keyword, String excludedUserId, long pageNum, long pageSize) {
        Long tenantId = requireTenantId();
        Long excludedId = parseId(excludedUserId);
        if (excludedId == null || pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("转办或委派目标查询参数不合法");
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<User> page = userService.lambdaQuery().eq(User::getTenantId, tenantId)
                .eq(User::getUsable, CommonConst.YES)
                .ne(User::getId, excludedId)
                .and(normalizedKeyword != null, query -> query
                        .like(User::getName, normalizedKeyword).or().like(User::getAccount, normalizedKeyword))
                .orderByAsc(User::getName)
                .page(new Page<>(pageNum, pageSize));
        return toDirectoryPage(page, page.getRecords().stream().map(user -> new WorkflowDirectoryEntry(
                user.getId().toString(), user.getAccount(), displayName(user), WorkflowDirectoryType.USER, null)).toList());
    }

    private static WorkflowUserProfile toProfile(User user) {
        String name = user.getName() == null || user.getName().isBlank() ? user.getAccount() : user.getName();
        return new WorkflowUserProfile(user.getId().toString(), name,
                user.getDeptId() == null ? null : user.getDeptId().toString());
    }

    private static WorkflowDepartmentInfo toDepartmentInfo(Dept department) {
        return new WorkflowDepartmentInfo(department.getId().toString(), department.getDeptName(),
                department.getParentId() == null ? null : department.getParentId().toString());
    }

    private static String displayName(User user) {
        return user.getName() == null || user.getName().isBlank() ? user.getAccount() : user.getName();
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private static WorkflowDirectoryPage toDirectoryPage(Page<?> page, List<WorkflowDirectoryEntry> records) {
        return new WorkflowDirectoryPage(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    private static boolean isActiveUser(User user, Long tenantId) {
        return user != null && Objects.equals(user.getTenantId(), tenantId)
                && CommonConst.YES.equals(user.getUsable());
    }

    private static boolean isActiveTenantData(Long dataTenantId, Integer usable, Long tenantId) {
        return Objects.equals(dataTenantId, tenantId) && CommonConst.YES.equals(usable);
    }

    private Long requireTenantId() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Flow 租户上下文不能为空");
        }
        return tenantId;
    }

    private static List<Long> parseIds(Collection<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        return values.stream().map(UpmWorkflowDataProvider::parseId).filter(Objects::nonNull).distinct().toList();
    }

    private static Long parseId(String value) {
        if (value == null || !value.matches("[0-9]{1,19}")) return null;
        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static List<String> toStringIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).toList();
    }
}
