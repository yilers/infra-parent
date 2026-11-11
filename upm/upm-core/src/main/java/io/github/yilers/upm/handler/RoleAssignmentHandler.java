package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserRole;
import io.github.yilers.upm.i18n.UpmManagementMessage;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 统一角色分配的业务边界；内部初始化使用的关系写入服务不依赖登录态。 */
@Component
@RequiredArgsConstructor
public class RoleAssignmentHandler {
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final UserService userService;
    private final CommonHandler commonHandler;

    /** 当前租户的启用角色才可新增分配，平台管理员角色只能由平台管理员分配。 */
    public boolean canAssign(Role role) {
        return role != null && RequestContextHolder.getTenantId() != null
                && Objects.equals(RequestContextHolder.getTenantId(), role.getTenantId())
                && !CommonConst.YES.equals(role.getDeleted())
                && CommonConst.YES.equals(role.getUsable())
                && (!CommonConst.PLATFORM_ADMIN_ROLE_CODE.equals(role.getRoleCode())
                    || StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE));
    }

    public void checkRole(Role role) {
        if (!canAssign(role)) {
            throw new CommonException(UpmManagementMessage.ROLE_ASSIGNMENT_FORBIDDEN);
        }
    }

    /**
     * 校验用户表单提交的角色，并保留已有但不可管理的关联。
     * 使用原始关联查询，避免缓存或启用状态过滤导致停用、受保护角色被意外删除。
     */
    public List<Long> rolesForUser(Long userId, List<Long> requestedIds) {
        Set<Long> requested = new LinkedHashSet<>(requestedIds == null ? List.of() : requestedIds);
        Set<Long> existing = userId == null ? Set.of() : userRoleService.list(
                Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId)).stream()
                .map(UserRole::getRoleId).collect(Collectors.toSet());
        Set<Long> allIds = new HashSet<>(existing);
        allIds.addAll(requested);
        if (allIds.contains(null)) {
            throw new CommonException(UpmManagementMessage.ROLE_ASSIGNMENT_FORBIDDEN);
        }
        Map<Long, Role> roles = allIds.isEmpty() ? Map.of() : roleService.listByIds(allIds).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));
        for (Long roleId : requested) {
            if (!existing.contains(roleId)) {
                checkRole(roles.get(roleId));
            }
        }
        // 已经持有的不可分配角色只保留，不允许通过遗漏字段撤销它。
        for (Long roleId : existing) {
            if (!canAssign(roles.get(roleId))) {
                requested.add(roleId);
            }
        }
        return new ArrayList<>(requested);
    }

    /** 替换角色的可管理用户集合，范围外的已有用户关联必须原样保留。 */
    public List<Long> usersForRole(List<Long> requestedIds, List<Long> existingIds) {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new CommonException(UpmManagementMessage.DATA_FORBIDDEN);
        }
        Set<Long> requested = new LinkedHashSet<>(requestedIds == null ? List.of() : requestedIds);
        Set<Long> existing = new HashSet<>(existingIds);
        Set<Long> allIds = new HashSet<>(existing);
        allIds.addAll(requested);
        if (allIds.contains(null)) {
            throw new CommonException(UpmManagementMessage.DATA_FORBIDDEN);
        }
        Map<Long, User> users = allIds.isEmpty() ? Map.of() : userService.listByIds(allIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> departments = commonHandler.findDataScopeByUserId(StpUtil.getLoginIdAsLong(), null);
        boolean platformAdmin = StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE);
        for (Long userId : allIds) {
            User user = users.get(userId);
            boolean manageable = user != null && Objects.equals(tenantId, user.getTenantId())
                    && !CommonConst.YES.equals(user.getDeleted())
                    && (platformAdmin || CommonConst.YES.equals(user.getOperable()))
                    && (departments == null || departments.contains(user.getDeptId()));
            if (!manageable) {
                if (existing.contains(userId)) {
                    requested.add(userId);
                } else {
                    throw new CommonException(UpmManagementMessage.DATA_FORBIDDEN);
                }
            } else if (requested.contains(userId) && !existing.contains(userId)
                    && !CommonConst.YES.equals(user.getUsable())) {
                throw new CommonException(UpmManagementMessage.DATA_FORBIDDEN);
            }
        }
        return new ArrayList<>(requested);
    }
}
