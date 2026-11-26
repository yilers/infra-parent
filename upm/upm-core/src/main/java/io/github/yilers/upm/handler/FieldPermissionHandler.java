package io.github.yilers.upm.handler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.api.field.*;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.request.RoleFieldRequest;
import io.github.yilers.upm.response.RoleFieldResponse;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.i18n.CommonMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 字段目录同步、角色授权及当前人策略；租户由服务端认证恢复，查询不采用客户端租户ID。 */
@Component
@RequiredArgsConstructor
public class FieldPermissionHandler implements FieldPermissionProvider {
    private final FieldService fieldService;
    private final RoleFieldService roleFieldService;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final TenantService tenantService;
    private final List<FieldDefinitionProvider> definitions;

    private List<Role> currentRoles() {
        Long userId = RequestContextHolder.getUserId();
        if (userId == null || RequestContextHolder.getTenantId() == null) {
            return List.of();
        }
        var ids = userRoleService.list(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).distinct().toList();
        return ids.isEmpty() ? List.of() : roleService.listByIds(ids).stream()
                .filter(r -> Objects.equals(r.getTenantId(), RequestContextHolder.getTenantId()))
                .filter(r -> CommonConst.YES.equals(r.getUsable()) && !CommonConst.YES.equals(r.getDeleted())).toList();
    }

    /** 内置管理员具有本租户字段管理权，不绕过接口功能权限或行数据权限。 */
    private boolean isAdmin(List<Role> roles) {
        return roles.stream().anyMatch(r -> CommonConst.PLATFORM_ADMIN_ROLE_CODE.equals(r.getRoleCode())
                || CommonConst.TENANT_ADMIN_ROLE_CODE.equals(r.getRoleCode()));
    }

    private List<FieldDefinition> catalog() {
        Map<String, FieldDefinition> result = new TreeMap<>();
        for (var source : definitions) {
            for (var definition : source.definitions()) {
                var previous = result.putIfAbsent(definition.key(), definition);
                if (previous != null && !previous.equals(definition)) {
                    throw invalid();
                }
            }
        }
        return List.copyOf(result.values());
    }

    @Override
    public Map<String, FieldAccess> currentAccess() {
        var roles = currentRoles();
        if (roles.isEmpty()) {
            return Map.of();
        }
        Map<String, FieldAccess> result = new HashMap<>();
        if (isAdmin(roles)) {
            for (var definition : catalog()) {
                result.put(definition.key(), new FieldAccess(FieldViewMode.FULL, definition.writeSupported()));
            }
            return result;
        }
        var fields = fieldService.list().stream()
                .filter(f -> Objects.equals(f.getTenantId(), RequestContextHolder.getTenantId()))
                .filter(f -> CommonConst.YES.equals(f.getUsable()))
                .collect(Collectors.toMap(Field::getId, Function.identity()));
        if (fields.isEmpty()) {
            return Map.of();
        }
        var roleIds = roles.stream().map(Role::getId).toList();
        for (var grant : roleFieldService.list(Wrappers.<RoleField>lambdaQuery().in(RoleField::getRoleId, roleIds))) {
            Field field = fields.get(grant.getFieldId());
            if (field == null) {
                continue;
            }
            String key = field.getResourceCode() + ":" + field.getFieldCode();
            var access = new FieldAccess(FieldViewMode.valueOf(grant.getViewMode()),
                    Boolean.TRUE.equals(field.getWriteSupported()) && Boolean.TRUE.equals(grant.getEditable()));
            result.merge(key, access, FieldAccess::union);
        }
        return result;
    }

    /** 显式同步当前租户代码目录，不接收前端提交的任意字段声明，不依赖应用配置。 */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void synchronize() {
        if (RequestContextHolder.getTenantId() == null) {
            throw invalid();
        }
        // 目录不使用数据库唯一约束。先锁定租户，再查询未删除记录，防止并发检查后重复插入。
        // 锁必须覆盖整个同步事务，不能在查询与新增之间释放。
        var tenant = tenantService.getOne(Wrappers.<Tenant>lambdaQuery()
                .eq(Tenant::getId, RequestContextHolder.getTenantId()).last("FOR UPDATE"));
        if (tenant == null || !CommonConst.YES.equals(tenant.getUsable())) {
            throw invalid();
        }
        for (var definition : catalog()) {
            // 使用当前读，兼容MySQL可重复读隔离级别；逻辑删除记录不占用业务编码。
            var field = fieldService.getOne(Wrappers.<Field>lambdaQuery()
                    .eq(Field::getTenantId, RequestContextHolder.getTenantId())
                    .eq(Field::getResourceCode, definition.resource()).eq(Field::getFieldCode, definition.field())
                    .last("FOR UPDATE"));
            if (field == null) {
                field = new Field();
                field.setResourceCode(definition.resource());
                field.setFieldCode(definition.field());
                field.setTenantId(RequestContextHolder.getTenantId());
                field.setVersion(1);
                field.setUsable(CommonConst.YES);
                field.setOperable(CommonConst.NO);
                field.setDeleted(CommonConst.NO);
                field.setSortNumber(0);
            }
            field.setResourceName(definition.resourceName());
            field.setFieldName(definition.name());
            field.setMaskType(definition.mask().name());
            field.setWriteSupported(definition.writeSupported());
            if (!fieldService.saveOrUpdate(field)) {
                throw invalid();
            }
        }
    }

    private Role targetRole(Long roleId) {
        var role = roleService.getById(roleId);
        var roles = currentRoles();
        if (role == null || !Objects.equals(role.getTenantId(), RequestContextHolder.getTenantId())
                || !CommonConst.YES.equals(role.getUsable())
                || CommonConst.NO.equals(role.getOperable()) && !isAdmin(roles)
                || CommonConst.PLATFORM_ADMIN_ROLE_CODE.equals(role.getRoleCode())
                    && roles.stream().noneMatch(r -> CommonConst.PLATFORM_ADMIN_ROLE_CODE.equals(r.getRoleCode()))) {
            throw invalid();
        }
        return role;
    }

    public RoleFieldResponse findByRole(Long roleId) {
        var role = targetRole(roleId);
        var current = currentAccess();
        var grants = roleFieldService.list(Wrappers.<RoleField>lambdaQuery().eq(RoleField::getRoleId, roleId)).stream()
                .collect(Collectors.toMap(RoleField::getFieldId, Function.identity()));
        var rows = new ArrayList<RoleFieldResponse.Row>();
        for (var field : fieldService.list()) {
            if (!CommonConst.YES.equals(field.getUsable())
                    || !Objects.equals(field.getTenantId(), RequestContextHolder.getTenantId())) continue;
            var limit = current.getOrDefault(field.getResourceCode() + ":" + field.getFieldCode(), FieldAccess.HIDDEN);
            var grant = grants.get(field.getId());
            var existing = grant == null ? FieldAccess.HIDDEN : new FieldAccess(FieldViewMode.valueOf(grant.getViewMode()), Boolean.TRUE.equals(grant.getEditable()));
            boolean grantable = limit.viewMode() != FieldViewMode.HIDDEN
                    && limit.viewMode().union(existing.viewMode()) == limit.viewMode() && (!existing.editable() || limit.editable());
            rows.add(new RoleFieldResponse.Row(field.getId(), field.getResourceCode(), field.getResourceName(),
                    field.getFieldCode(), field.getFieldName(), FieldMaskType.valueOf(field.getMaskType()), Boolean.TRUE.equals(field.getWriteSupported()),
                    existing.viewMode(), existing.editable(), limit.viewMode(), grantable, limit.editable()));
        }
        return new RoleFieldResponse(role.getVersion(), rows);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(RoleFieldRequest request) {
        targetRole(request.roleId());
        if (!roleService.update(Wrappers.<Role>lambdaUpdate().eq(Role::getId, request.roleId())
                .eq(Role::getVersion, request.version()).set(Role::getVersion, request.version() + 1))) {
            throw new CommonException(CommonMessage.UPDATE_CONFLICT);
        }
        var rows = findByRole(request.roleId()).fields().stream().collect(Collectors.toMap(RoleFieldResponse.Row::fieldId, Function.identity()));
        Set<Long> visited = new HashSet<>();
        for (var requested : request.fields()) {
            var row = rows.get(requested.fieldId());
            if (row == null || !row.grantable() || !visited.add(requested.fieldId())
                    || row.maxViewMode().union(requested.viewMode()) != row.maxViewMode()
                    || requested.viewMode() == FieldViewMode.MASKED && row.maskType() == FieldMaskType.NONE
                    || requested.editable() && (requested.viewMode() != FieldViewMode.FULL || !row.writeSupported() || !row.grantEditable())) {
                throw invalid();
            }
            // 角色版本更新已持有角色行锁；仅替换本次可管理的关联，不清除范围外授权。
            roleFieldService.remove(Wrappers.<RoleField>lambdaQuery()
                    .eq(RoleField::getRoleId, request.roleId()).eq(RoleField::getFieldId, requested.fieldId()));
            if (requested.viewMode() == FieldViewMode.HIDDEN) {
                continue;
            }
            var grant = new RoleField();
            grant.setTenantId(RequestContextHolder.getTenantId());
            grant.setRoleId(request.roleId());
            grant.setFieldId(requested.fieldId());
            grant.setViewMode(requested.viewMode().name());
            grant.setEditable(requested.editable());
            if (!roleFieldService.save(grant)) {
                throw invalid();
            }
        }
    }

    private CommonException invalid() {
        return new CommonException(CommonMessage.FIELD_CONFIG_INVALID);
    }
}
