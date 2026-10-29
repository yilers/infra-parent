package io.github.yilers.upm.i18n;

import io.github.yilers.web.i18n.MessageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 租户、部门、角色和菜单管理的业务提示。 */
@Getter
@RequiredArgsConstructor
public enum UpmManagementMessage implements MessageCode {

    // 租户管理
    TENANT_PLATFORM_DELETE("upm.management.tenant-platform-delete", "平台租户不能删除"),
    TENANT_NOT_FOUND("upm.management.tenant-not-found", "租户不存在"),
    TENANT_NOT_OPERABLE("upm.management.tenant-not-operable", "该租户不可操作"),
    TENANT_DELETE_CONFLICT("upm.management.tenant-delete-conflict", "租户删除失败，数据已经变更"),
    TENANT_DELETE_FORBIDDEN("upm.management.tenant-delete-forbidden", "只有平台管理员可以删除租户"),
    TENANT_CODE_EXISTS("upm.management.tenant-code-exists", "租户编码已存在"),
    TENANT_CREATE_FAILED("upm.management.tenant-create-failed", "租户创建失败"),
    TENANT_TEMPLATE_APP_MISSING("upm.management.tenant-template-app-missing", "模板菜单所属应用不存在"),
    DATA_FORBIDDEN("upm.management.data-forbidden", "越权操作"),
    TENANT_UPDATE_FAILED("upm.management.tenant-update-failed", "更新失败 刷新重试"),
    // 登录策略
    POLICY_SAVE_FAILED("upm.management.policy-save-failed", "登录策略保存失败"),
    POLICY_CONFLICT("upm.management.policy-conflict", "登录策略已经变更，请刷新后重试"),
    POLICY_DELETE_FAILED("upm.management.policy-delete-failed", "登录策略删除失败，请刷新后重试"),
    POLICY_INIT_FAILED("upm.management.policy-init-failed", "租户默认登录策略创建失败"),
    // 租户菜单同步
    SYNC_APP_FAILED("upm.management.sync-app-failed", "同步应用失败"),
    SYNC_APP_CONFLICT("upm.management.sync-app-conflict", "同步应用失败，数据已经变更"),
    SYNC_DUPLICATE_SOURCE("upm.management.sync-duplicate-source", "目标租户存在重复的模板菜单来源"),
    SYNC_LEGACY_TENANT("upm.management.sync-legacy-tenant", "该租户为改造前创建的历史租户，请重新创建后再同步"),
    SYNC_MENU_FAILED("upm.management.sync-menu-failed", "同步菜单失败"),
    SYNC_MENU_CONFLICT("upm.management.sync-menu-conflict", "同步菜单失败，数据已经变更"),
    SYNC_NO_TENANT_ADMIN("upm.management.sync-no-tenant-admin", "目标租户缺少租户管理员角色"),
    SYNC_ADMIN_MENU_MISSING("upm.management.sync-admin-menu-missing", "租户管理员权限对应的菜单不存在"),
    SYNC_ADMIN_FAILED("upm.management.sync-admin-failed", "同步租户管理员权限失败"),
    SYNC_TEMPLATE_ADMIN_MISSING("upm.management.sync-template-admin-missing", "平台模板缺少租户管理员角色"),
    SYNC_FORBIDDEN("upm.management.sync-forbidden", "只有平台管理员可以同步租户菜单"),
    SYNC_PLATFORM_FORBIDDEN("upm.management.sync-platform-forbidden", "不能同步平台模板租户"),
    SYNC_TARGET_MISSING("upm.management.sync-target-missing", "目标租户不存在"),
    SYNC_TEMPLATE_APP_FAILED("upm.management.sync-template-app-failed", "模板菜单所属应用同步失败"),
    SYNC_TEMPLATE_PARENT_MISSING("upm.management.sync-template-parent-missing", "模板菜单父级不存在"),
    // 部门与主管
    DEPT_ID_REQUIRED("upm.management.dept-id-required", "部门id不能为空"),
    DEPT_NOT_FOUND("upm.management.dept-not-found", "部门不存在"),
    DEPT_CODE_EXISTS("upm.management.dept-code-exists", "部门编码重复"),
    DEPT_HAS_USERS("upm.management.dept-has-users", "部门下有用户，不能删除"),
    DEPT_HAS_CHILDREN("upm.management.dept-has-children", "部门下有子部门，不能删除"),
    DEPT_HAS_LEADERS("upm.management.dept-has-leaders", "部门已设置主管，请先撤销主管任职"),
    DEPT_DIFFERENT_PARENT("upm.management.dept-different-parent", "不同父级不能移动"),
    DEPT_LEADER_DUPLICATE("upm.management.dept-leader-duplicate", "主管和副主管不能重复"),
    DEPT_LEADER_LIMIT("upm.management.dept-leader-limit", "一个部门最多设置20位负责人"),
    DEPT_LEADER_UNAVAILABLE("upm.management.dept-leader-unavailable", "主管用户不存在或已停用"),
    DEPT_LEADER_UPDATE_FAILED("upm.management.dept-leader-update-failed", "主管任职更新失败"),
    DEPT_LEADER_SAVE_FAILED("upm.management.dept-leader-save-failed", "主管任职保存失败"),
    // 角色管理
    ROLE_CODE_DUPLICATE("upm.management.role-code-duplicate", "角色编号重复"),
    ROLE_DEPT_REQUIRED("upm.management.role-dept-required", "必须选择部门"),
    ROLE_NOT_OPERABLE("upm.management.role-not-operable", "该角色不可操作"),
    ROLE_CODE_EXISTS("upm.management.role-code-exists", "角色编码已存在"),
    ROLE_HAS_USERS("upm.management.role-has-users", "该角色下有用户，请先删除用户"),
    ROLE_NOT_FOUND("upm.management.role-not-found", "角色不存在"),
    ROLE_PERMISSION_OUTSIDE_SCOPE("upm.management.role-permission-outside-scope", "权限不属于当前应用、设备端或超出可授权范围"),
    ROLE_SCOPE_INVALID("upm.management.role-scope-invalid", "角色或权限范围不正确"),
    // 菜单和按钮权限
    MENU_CODE_REQUIRED("upm.management.menu-code-required", "按钮权限编码不能为空"),
    MENU_CODE_EXISTS("upm.management.menu-code-exists", "按钮权限编码已存在"),
    MENU_RESOURCE_NOT_FOUND("upm.management.menu-resource-not-found", "资源不存在"),
    MENU_RESOURCE_NOT_OPERABLE("upm.management.menu-resource-not-operable", "该资源不可操作"),
    MENU_TEMPLATE_DELETE("upm.management.menu-template-delete", "平台下发的菜单不能删除，可通过平台模板统一停用"),
    MENU_HAS_CHILDREN("upm.management.menu-has-children", "存在子资源，不能直接删除"),
    MENU_HAS_ROLES("upm.management.menu-has-roles", "有角色绑定，不能直接删除"),
    MENU_MOVE_ACROSS_CONTEXT("upm.management.menu-move-across-context", "不能跨应用或设备端移动菜单"),
    MENU_MOVE_MISSING("upm.management.menu-move-missing", "移动数据不存在"),
    MENU_SORT_ACROSS_CONTEXT("upm.management.menu-sort-across-context", "不能跨应用或设备端排序"),
    MENU_SORT_NOT_SIBLINGS("upm.management.menu-sort-not-siblings", "不能非同级移动"),
    MENU_QUERY_FORBIDDEN("upm.management.menu-query-forbidden", "无权查询其他应用菜单"),
    MENU_PARENT_CYCLE("upm.management.menu-parent-cycle", "不能选择自身或子菜单作为父级"),
    MENU_PARENT_CONTEXT("upm.management.menu-parent-context", "父菜单必须属于同一应用和设备端"),
    MENU_BUTTON_PARENT("upm.management.menu-button-parent", "按钮不能作为父菜单"),
    // 共用数据操作
    DATA_NOT_OPERABLE("upm.management.data-not-operable", "数据不可操作"),
    DATA_UPDATE_CONFLICT("upm.management.data-update-conflict", "更新失败 数据已经变更"),
    DATA_NOT_FOUND("upm.management.data-not-found", "数据不存在"),
    DATA_ID_NOT_FOUND("upm.management.data-id-not-found", "id不存在"),
    DATA_SORT_CONFLICT("upm.management.data-sort-conflict", "排序失败 数据已经变更");

    private final String key;
    private final String defaultMessage;

    @Override
    public String getBasename() {
        return "i18n/upm-messages";
    }
}
