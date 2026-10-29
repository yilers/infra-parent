package io.github.yilers.upm.i18n;

import io.github.yilers.web.i18n.MessageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** UPM 模块的用户提示。 */
@Getter
@RequiredArgsConstructor
public enum UpmMessage implements MessageCode {

    // 登录认证
    LOGIN_INVALID_CREDENTIALS("upm.login.invalid-credentials", "账号或密码错误"),
    LOGIN_TENANT_DISABLED("upm.login.tenant-disabled", "租户已禁用"),
    LOGIN_ACCOUNT_DISABLED("upm.login.account-disabled", "账号暂不可用"),
    LOGIN_ACCOUNT_LOCKED("upm.login.account-locked", "登录失败次数过多，请{0}分钟后再试"),

    // 用户管理
    USER_NOT_FOUND("upm.user.not-found", "用户不存在"),
    USER_ACCOUNT_EXISTS("upm.user.account-exists", "账号已存在"),
    USER_NOT_OPERABLE("upm.user.not-operable", "数据不可操作"),
    USER_CANNOT_OPERATE_SELF("upm.user.cannot-operate-self", "不能操作自己"),
    USER_CANNOT_DELETE_SELF("upm.user.cannot-delete-self", "不能删除自己"),
    USER_DEPT_LEADER("upm.user.dept-leader", "该用户仍担任部门主管，请先调整部门主管"),
    USER_OLD_PASSWORD_INCORRECT("upm.user.old-password-incorrect", "旧密码错误"),
    USER_PASSWORD_UPDATE_FAILED("upm.user.password-update-failed", "修改密码失败 刷新重试"),
    USER_UPDATE_CONFLICT_REFRESH("upm.user.update-conflict-refresh", "更新失败 数据已经变更，请刷新后重试"),
    USER_TENANT_MISMATCH("upm.user.tenant-mismatch", "用户不存在或不属于当前租户"),
    USER_SCOPE_INVALID_PATH("upm.user.scope-invalid-path", "接口路径格式错误或重复"),
    USER_SCOPE_DEPT_REQUIRED("upm.user.scope-dept-required", "自定义数据权限必须选择部门"),
    USER_SCOPE_DEPT_NOT_FOUND("upm.user.scope-dept-not-found", "部门不存在或不属于当前租户"),
    USER_SCOPE_DEPT_LIMIT("upm.user.scope-dept-limit", "自定义部门数量超出存储范围"),
    USER_BATCH_SAVE_FAILED("upm.user.batch-save-failed", "批量新增用户失败"),
    USER_BATCH_ROLE_SAVE_FAILED("upm.user.batch-role-save-failed", "批量保存用户角色失败");

    private final String key;
    private final String defaultMessage;

    @Override
    public String getBasename() {
        return "i18n/upm-messages";
    }
}
