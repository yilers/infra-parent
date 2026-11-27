package io.github.yilers.infra.notice;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.notice.model.*;
import io.github.yilers.notice.model.NoticeTypes.DirectoryType;
import io.github.yilers.notice.service.NoticeDataProvider;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.context.RequestContextHolder;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.List;

/** 单体UPM通知数据适配，只执行同租户基础查询。 接收范围展开、去重、快照和消息内容均由notice-core负责；微服务仅替换查询实现。 */
@Component
@RequiredArgsConstructor
public class UpmNoticeDataProvider implements NoticeDataProvider {
    private final TenantService tenants;
    private final UserService users;
    private final DeptService departments;
    private final RoleService roles;
    private final UserRoleService userRoles;

    @Override
    public boolean isTenantAvailable() {
        var tenant = tenants.getById(tenant());
        return tenant != null && CommonConst.YES.equals(tenant.getUsable());
    }

    @Override
    public List<NoticeUser> findAllUsers() {
        return users
                .lambdaQuery()
                .eq(User::getTenantId, tenant())
                .eq(User::getUsable, CommonConst.YES)
                .orderByAsc(User::getId)
                .list()
                .stream()
                .map(this::profile)
                .toList();
    }

    @Override
    public List<NoticeUser> findUsersByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        // 按500个ID查询，避免远程适配或数据库IN参数过大。
        java.util.List<NoticeUser> result = new java.util.ArrayList<>();
        for (int offset = 0; offset < ids.size(); offset += 500)
            result.addAll(
                    users
                            .lambdaQuery()
                            .eq(User::getTenantId, tenant())
                            .eq(User::getUsable, CommonConst.YES)
                            .in(User::getId, ids.subList(offset, Math.min(offset + 500, ids.size())))
                            .list()
                            .stream()
                            .map(this::profile)
                            .toList());
        return result;
    }

    @Override
    public List<Long> findUserIdsByRoleIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        var validIds =
                roles
                        .lambdaQuery()
                        .eq(Role::getTenantId, tenant())
                        .eq(Role::getUsable, CommonConst.YES)
                        .in(Role::getId, ids)
                        .list()
                        .stream()
                        .map(Role::getId)
                        .toList();
        if (validIds.isEmpty()) return List.of();
        return userRoles
                .list(
                        Wrappers.<UserRole>lambdaQuery()
                                .eq(UserRole::getTenantId, tenant())
                                .in(UserRole::getRoleId, validIds))
                .stream()
                .map(UserRole::getUserId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> findUserIdsByDepartmentIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return users
                .lambdaQuery()
                .eq(User::getTenantId, tenant())
                .eq(User::getUsable, CommonConst.YES)
                .in(User::getDeptId, ids)
                .list()
                .stream()
                .map(User::getId)
                .toList();
    }

    @Override
    public List<NoticeDepartment> findDepartments() {
        return departments
                .lambdaQuery()
                .eq(Dept::getTenantId, tenant())
                .eq(Dept::getUsable, CommonConst.YES)
                .list()
                .stream()
                .map(dept -> new NoticeDepartment(dept.getId(), dept.getParentId()))
                .toList();
    }

    @Override
    public List<NoticeDirectoryEntry> findEntries(DirectoryType type, List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return switch (type) {
            case USER ->
                    findUsersByIds(ids).stream()
                            .map(user -> new NoticeDirectoryEntry(user.id(), user.name(), user.email()))
                            .toList();
            case DEPARTMENT ->
                    departments
                            .lambdaQuery()
                            .eq(Dept::getTenantId, tenant())
                            .eq(Dept::getUsable, CommonConst.YES)
                            .in(Dept::getId, ids)
                            .list()
                            .stream()
                            .map(dept -> new NoticeDirectoryEntry(dept.getId(), dept.getDeptName(), null))
                            .toList();
            case ROLE ->
                    roles
                            .lambdaQuery()
                            .eq(Role::getTenantId, tenant())
                            .eq(Role::getUsable, CommonConst.YES)
                            .in(Role::getId, ids)
                            .list()
                            .stream()
                            .map(role -> new NoticeDirectoryEntry(role.getId(), role.getRoleName(), null))
                            .toList();
        };
    }

    @Override
    public NoticePage<NoticeDirectoryEntry> search(DirectoryType type, String keyword, int current, int size) {
        Long tenantId = tenant();
        String word = keyword == null ? "" : keyword.trim();
        return switch (type) {
            case USER -> {
                var page =
                        users.lambdaQuery()
                                .eq(User::getTenantId, tenantId)
                                .eq(User::getUsable, CommonConst.YES)
                                .and(
                                        !word.isBlank(),
                                        q -> q.like(User::getName, word).or().like(User::getAccount, word))
                                .orderByAsc(User::getId)
                                .page(new Page<>(current, size));
                yield new NoticePage<>(
                        page.getTotal(),
                        page.getRecords().stream()
                                .map(user -> new NoticeDirectoryEntry(user.getId(), name(user), user.getEmail()))
                                .toList());
            }
            case DEPARTMENT -> {
                var page =
                        departments
                                .lambdaQuery()
                                .eq(Dept::getTenantId, tenantId)
                                .eq(Dept::getUsable, CommonConst.YES)
                                .like(!word.isBlank(), Dept::getDeptName, word)
                                .orderByAsc(Dept::getId)
                                .page(new Page<>(current, size));
                yield new NoticePage<>(
                        page.getTotal(),
                        page.getRecords().stream()
                                .map(dept -> new NoticeDirectoryEntry(dept.getId(), dept.getDeptName(), null))
                                .toList());
            }
            case ROLE -> {
                var page =
                        roles.lambdaQuery()
                                .eq(Role::getTenantId, tenantId)
                                .eq(Role::getUsable, CommonConst.YES)
                                .like(!word.isBlank(), Role::getRoleName, word)
                                .orderByAsc(Role::getId)
                                .page(new Page<>(current, size));
                yield new NoticePage<>(
                        page.getTotal(),
                        page.getRecords().stream()
                                .map(role -> new NoticeDirectoryEntry(role.getId(), role.getRoleName(), null))
                                .toList());
            }
        };
    }

    private NoticeUser profile(User user) {
        return new NoticeUser(user.getId(), name(user), user.getEmail());
    }

    private String name(User user) {
        return user.getName() == null || user.getName().isBlank() ? user.getAccount() : user.getName();
    }

    private Long tenant() {
        Long id = RequestContextHolder.getTenantId();
        if (id == null) throw new IllegalStateException("通知租户上下文不能为空");
        return id;
    }
}
