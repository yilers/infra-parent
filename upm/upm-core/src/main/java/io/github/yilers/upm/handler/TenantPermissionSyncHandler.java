package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Application;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.RolePermission;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.response.TenantPermissionSyncResponse;
import io.github.yilers.upm.service.ApplicationService;
import io.github.yilers.upm.service.PermissionService;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 将平台模板租户的应用、菜单按钮和租户管理员权限同步到目标租户。
 *
 * <p>应用使用不可变的编码建立对应关系；菜单使用 {@link Permission#getSourceId()} 记录模板来源。
 * 同步只维护平台下发的菜单，租户自行创建且来源为空的数据不会被修改。</p>
 */
@Component
@RequiredArgsConstructor
public class TenantPermissionSyncHandler {

    private final TenantService tenantService;
    private final ApplicationService applicationService;
    private final PermissionService permissionService;
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;
    private final UserRoleService userRoleService;

    /**
     * 预览目标租户与平台模板之间的差异，不修改任何数据。
     */
    public TenantPermissionSyncResponse preview(Long tenantId) {
        checkTargetTenant(tenantId);
        TemplateSnapshot template = loadTemplate();
        return withTenant(tenantId, () -> synchronize(template, tenantId, false));
    }

    /**
     * 同步目标租户的应用菜单，并校准租户管理员对平台菜单的授权范围。
     */
    @Transactional(rollbackFor = Exception.class)
    public TenantPermissionSyncResponse sync(Long tenantId) {
        checkTargetTenant(tenantId);
        TemplateSnapshot template = loadTemplate();
        return withTenant(tenantId, () -> synchronize(template, tenantId, true));
    }

    private TenantPermissionSyncResponse synchronize(TemplateSnapshot template, Long tenantId, boolean apply) {
        TenantPermissionSyncResponse response = new TenantPermissionSyncResponse();
        Map<Long, String> templateApplicationCodeMap = template.applications().stream()
                .collect(Collectors.toMap(Application::getId, Application::getCode));
        Map<String, Application> targetApplicationMap = applicationService.list().stream()
                .collect(Collectors.toMap(Application::getCode, application -> application));

        synchronizeApplications(template.applications(), targetApplicationMap, tenantId, response, apply);
        synchronizePermissions(template.permissions(), templateApplicationCodeMap, targetApplicationMap,
                tenantId, response, apply);
        synchronizeTenantAdminPermissions(template.tenantAdminPermissionIds(), response, tenantId, apply);
        return response;
    }

    private void synchronizeApplications(List<Application> templateApplications,
                                         Map<String, Application> targetApplicationMap,
                                         Long tenantId,
                                         TenantPermissionSyncResponse response,
                                         boolean apply) {
        for (Application template : templateApplications) {
            Application target = targetApplicationMap.get(template.getCode());
            if (target == null) {
                response.setApplicationAddCount(response.getApplicationAddCount() + 1);
                if (apply) {
                    target = newApplication(template, tenantId);
                    if (!applicationService.save(target)) {
                        throw new CommonException("同步应用失败");
                    }
                    targetApplicationMap.put(target.getCode(), target);
                }
                continue;
            }
            if (applicationChanged(template, target)) {
                response.setApplicationUpdateCount(response.getApplicationUpdateCount() + 1);
                if (apply) {
                    copyApplicationFields(template, target);
                    if (!applicationService.updateById(target)) {
                        throw new CommonException("同步应用失败，数据已经变更");
                    }
                }
            }
        }
    }

    private void synchronizePermissions(List<Permission> templatePermissions,
                                        Map<Long, String> templateApplicationCodeMap,
                                        Map<String, Application> targetApplicationMap,
                                        Long tenantId,
                                        TenantPermissionSyncResponse response,
                                        boolean apply) {
        List<Permission> targetPermissions = permissionService.list();
        Map<Long, Permission> targetBySourceId = new HashMap<>();
        for (Permission permission : targetPermissions) {
            if (permission.getSourceId() != null && targetBySourceId.put(permission.getSourceId(), permission) != null) {
                throw new CommonException("目标租户存在重复的模板菜单来源");
            }
        }
        if (!targetPermissions.isEmpty() && targetBySourceId.isEmpty()) {
            throw new CommonException("该租户为改造前创建的历史租户，请重新创建后再同步");
        }

        Set<Long> templatePermissionIds = templatePermissions.stream()
                .map(Permission::getId).collect(Collectors.toSet());
        List<Permission> additions = new ArrayList<>();
        for (Permission template : templatePermissions) {
            if (targetBySourceId.containsKey(template.getId())) {
                continue;
            }
            response.setPermissionAddCount(response.getPermissionAddCount() + 1);
            if (apply) {
                Permission target = new Permission();
                target.setId(IdWorker.getId());
                target.setTenantId(tenantId);
                target.setSourceId(template.getId());
                target.setVersion(1);
                targetBySourceId.put(template.getId(), target);
                additions.add(target);
            }
        }

        List<Permission> updates = new ArrayList<>();
        if (apply) {
            for (Permission template : templatePermissions) {
                Permission target = targetBySourceId.get(template.getId());
                Long targetAppId = resolveTargetApplicationId(template, templateApplicationCodeMap, targetApplicationMap);
                Long targetParentId = resolveTargetParentId(template, targetBySourceId);
                boolean added = additions.contains(target);
                if (!added && permissionChanged(template, target, targetAppId, targetParentId)) {
                    response.setPermissionUpdateCount(response.getPermissionUpdateCount() + 1);
                    updates.add(target);
                }
                copyPermissionFields(template, target, targetAppId, targetParentId);
            }
        } else {
            for (Permission template : templatePermissions) {
                Permission target = targetBySourceId.get(template.getId());
                if (target == null) {
                    continue;
                }
                String applicationCode = templateApplicationCodeMap.get(template.getAppId());
                Application targetApplication = targetApplicationMap.get(applicationCode);
                Long targetAppId = targetApplication == null ? null : targetApplication.getId();
                Permission targetParent = template.getParentId() == null || template.getParentId() == 0L
                        ? null : targetBySourceId.get(template.getParentId());
                Long targetParentId = targetParent == null ? 0L : targetParent.getId();
                if (permissionChanged(template, target, targetAppId, targetParentId)) {
                    response.setPermissionUpdateCount(response.getPermissionUpdateCount() + 1);
                }
            }
        }

        List<Permission> disabled = targetPermissions.stream()
                .filter(permission -> permission.getSourceId() != null)
                .filter(permission -> !templatePermissionIds.contains(permission.getSourceId()))
                .filter(permission -> !CommonConst.NO.equals(permission.getUsable()))
                .toList();
        response.setPermissionDisableCount(disabled.size());
        if (!apply) {
            return;
        }
        if (!additions.isEmpty() && !permissionService.saveBatch(additions)) {
            throw new CommonException("同步菜单失败");
        }
        disabled.forEach(permission -> permission.setUsable(CommonConst.NO));
        updates.addAll(disabled);
        if (!updates.isEmpty() && !permissionService.updateBatchById(updates)) {
            throw new CommonException("同步菜单失败，数据已经变更");
        }
    }

    private void synchronizeTenantAdminPermissions(Set<Long> templatePermissionIds,
                                                   TenantPermissionSyncResponse response,
                                                   Long tenantId,
                                                   boolean apply) {
        Role tenantAdmin = roleService.findByRoleCode(CommonConst.TENANT_ADMIN_ROLE_CODE);
        if (tenantAdmin == null) {
            throw new CommonException("目标租户缺少租户管理员角色");
        }
        List<Permission> targetPermissions = permissionService.list();
        Map<Long, Permission> targetBySourceId = targetPermissions.stream()
                .filter(permission -> permission.getSourceId() != null)
                .collect(Collectors.toMap(Permission::getSourceId, permission -> permission));
        Map<Long, Permission> targetById = targetPermissions.stream()
                .collect(Collectors.toMap(Permission::getId, permission -> permission));
        Set<Long> currentSourceIds = rolePermissionService.findPermissionListByRoleId(tenantAdmin.getId()).stream()
                .map(Permission::getId)
                .map(targetById::get)
                .filter(Objects::nonNull)
                .map(Permission::getSourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> additionSourceIds = new LinkedHashSet<>(templatePermissionIds);
        additionSourceIds.removeAll(currentSourceIds);
        Set<Long> removalSourceIds = new LinkedHashSet<>(currentSourceIds);
        removalSourceIds.removeAll(templatePermissionIds);
        response.setTenantAdminPermissionAddCount(additionSourceIds.size());
        response.setTenantAdminPermissionRemoveCount(removalSourceIds.size());
        if (!apply) {
            return;
        }
        List<Long> removalIds = removalSourceIds.stream()
                .map(targetBySourceId::get).filter(Objects::nonNull).map(Permission::getId).toList();
        if (!removalIds.isEmpty()) {
            rolePermissionService.deleteByRoleIdAndPermissionIds(tenantAdmin.getId(), removalIds);
        }
        if (!additionSourceIds.isEmpty()) {
            List<RolePermission> relations = additionSourceIds.stream().map(sourceId -> {
                Permission permission = targetBySourceId.get(sourceId);
                if (permission == null) {
                    throw new CommonException("租户管理员权限对应的菜单不存在");
                }
                RolePermission relation = new RolePermission();
                relation.setRoleId(tenantAdmin.getId());
                relation.setPermissionId(permission.getId());
                relation.setTenantId(tenantId);
                relation.setDevice(permission.getDevice());
                return relation;
            }).toList();
            if (!rolePermissionService.saveBatch(relations)) {
                throw new CommonException("同步租户管理员权限失败");
            }
        }
        if (!additionSourceIds.isEmpty() || !removalSourceIds.isEmpty()) {
            userRoleService.findUserIdListByRoleId(tenantAdmin.getId()).forEach(userRoleService::cleanCache);
        }
    }

    private TemplateSnapshot loadTemplate() {
        return withTenant(CommonConst.PLATFORM_TENANT_ID, () -> {
            Role tenantAdmin = roleService.findByRoleCode(CommonConst.TENANT_ADMIN_ROLE_CODE);
            if (tenantAdmin == null) {
                throw new CommonException("平台模板缺少租户管理员角色");
            }
            Set<Long> permissionIds = rolePermissionService.findPermissionListByRoleId(tenantAdmin.getId()).stream()
                    .map(Permission::getId).collect(Collectors.toSet());
            return new TemplateSnapshot(applicationService.list(), permissionService.list(), permissionIds);
        });
    }

    private void checkTargetTenant(Long tenantId) {
        if (!CommonConst.PLATFORM_TENANT_ID.equals(RequestContextHolder.getTenantId())
                || !StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)) {
            throw new CommonException("只有平台管理员可以同步租户菜单");
        }
        if (tenantId == null || CommonConst.PLATFORM_TENANT_ID.equals(tenantId)) {
            throw new CommonException("不能同步平台模板租户");
        }
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw new CommonException("目标租户不存在");
        }
    }

    private Application newApplication(Application template, Long tenantId) {
        Application target = new Application();
        target.setTenantId(tenantId);
        target.setCode(template.getCode());
        target.setVersion(1);
        target.setDeleted(CommonConst.NO);
        target.setSsoEnabled(CommonConst.NO);
        target.setUsable(template.getUsable());
        copyApplicationFields(template, target);
        return target;
    }

    private void copyApplicationFields(Application template, Application target) {
        target.setName(template.getName());
        target.setIcon(template.getIcon());
        target.setLogo(template.getLogo());
        target.setHomeUrl(template.getHomeUrl());
        target.setDescription(template.getDescription());
        target.setSortNumber(template.getSortNumber());
        target.setOperable(template.getOperable());
    }

    private boolean applicationChanged(Application template, Application target) {
        return !Objects.equals(template.getName(), target.getName())
                || !Objects.equals(template.getIcon(), target.getIcon())
                || !Objects.equals(template.getLogo(), target.getLogo())
                || !Objects.equals(template.getHomeUrl(), target.getHomeUrl())
                || !Objects.equals(template.getDescription(), target.getDescription())
                || !Objects.equals(template.getSortNumber(), target.getSortNumber())
                || !Objects.equals(template.getOperable(), target.getOperable());
    }

    private Long resolveTargetApplicationId(Permission template,
                                            Map<Long, String> templateApplicationCodeMap,
                                            Map<String, Application> targetApplicationMap) {
        String code = templateApplicationCodeMap.get(template.getAppId());
        Application application = targetApplicationMap.get(code);
        if (application == null) {
            throw new CommonException("模板菜单所属应用同步失败");
        }
        return application.getId();
    }

    private Long resolveTargetParentId(Permission template, Map<Long, Permission> targetBySourceId) {
        if (template.getParentId() == null || template.getParentId() == 0L) {
            return 0L;
        }
        Permission parent = targetBySourceId.get(template.getParentId());
        if (parent == null) {
            throw new CommonException("模板菜单父级不存在");
        }
        return parent.getId();
    }

    private void copyPermissionFields(Permission template, Permission target, Long appId, Long parentId) {
        target.setAppId(appId);
        target.setParentId(parentId);
        target.setMenuIcon(template.getMenuIcon());
        target.setMenuUrl(template.getMenuUrl());
        target.setSortNumber(template.getSortNumber());
        target.setPermissionCode(template.getPermissionCode());
        target.setPermissionName(template.getPermissionName());
        target.setPermissionType(template.getPermissionType());
        target.setComponent(template.getComponent());
        target.setCache(template.getCache());
        target.setLink(template.getLink());
        target.setDevice(template.getDevice());
        target.setOperable(template.getOperable());
        target.setUsable(template.getUsable());
        target.setDeleted(CommonConst.NO);
    }

    private boolean permissionChanged(Permission template, Permission target, Long appId, Long parentId) {
        return !Objects.equals(appId, target.getAppId())
                || !Objects.equals(parentId, target.getParentId())
                || !Objects.equals(template.getMenuIcon(), target.getMenuIcon())
                || !Objects.equals(template.getMenuUrl(), target.getMenuUrl())
                || !Objects.equals(template.getSortNumber(), target.getSortNumber())
                || !Objects.equals(template.getPermissionCode(), target.getPermissionCode())
                || !Objects.equals(template.getPermissionName(), target.getPermissionName())
                || !Objects.equals(template.getPermissionType(), target.getPermissionType())
                || !Objects.equals(template.getComponent(), target.getComponent())
                || !Objects.equals(template.getCache(), target.getCache())
                || !Objects.equals(template.getLink(), target.getLink())
                || !Objects.equals(template.getDevice(), target.getDevice())
                || !Objects.equals(template.getOperable(), target.getOperable())
                || !Objects.equals(template.getUsable(), target.getUsable());
    }

    private <T> T withTenant(Long tenantId, Supplier<T> action) {
        Long previousTenantId = RequestContextHolder.getTenantId();
        try {
            RequestContextHolder.setTenantId(tenantId);
            return action.get();
        } finally {
            RequestContextHolder.setTenantId(previousTenantId);
        }
    }

    private record TemplateSnapshot(List<Application> applications,
                                    List<Permission> permissions,
                                    Set<Long> tenantAdminPermissionIds) {
    }
}
