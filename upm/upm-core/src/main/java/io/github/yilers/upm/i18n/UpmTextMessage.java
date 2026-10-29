package io.github.yilers.upm.i18n;

import io.github.yilers.web.i18n.MessageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 现有请求校验和批量录入结果使用的文本消息。 */
@Getter
@RequiredArgsConstructor
public enum UpmTextMessage implements MessageCode {

    ACCOUNT_REQUIRED("upm.text.account-required", "账号不能为空"),
    PASSWORD_REQUIRED("upm.text.password-required", "密码不能为空"),
    ID_REQUIRED("upm.text.id-required", "id不能为空"),
    DEPT_ID_REQUIRED("upm.text.dept-id-required", "部门id为空"),
    VERSION_REQUIRED("upm.text.version-required", "版本号不能为空"),
    USERS_REQUIRED("upm.text.users-required", "用户数据不能为空"),
    USERS_LIMIT("upm.text.users-limit", "单次最多新增500个用户"),
    OLD_PASSWORD_REQUIRED("upm.text.old-password-required", "旧秘密不能为空"),
    NEW_PASSWORD_REQUIRED("upm.text.new-password-required", "新密码不能为空"),
    NAME_REQUIRED("upm.text.name-required", "姓名不能为空"),
    NICKNAME_REQUIRED("upm.text.nickname-required", "昵称不能为空"),
    BATCH_DUPLICATE("upm.text.batch-duplicate", "本批次账号重复"),
    BATCH_ACCOUNT_REQUIRED("upm.text.batch-account-required", "用户账号不能为空"),
    BATCH_ACCOUNT_LENGTH("upm.text.batch-account-length", "用户账号不能超过50个字符"),
    BATCH_NAME_LENGTH("upm.text.batch-name-length", "姓名不能超过50个字符"),
    BATCH_NICKNAME_LENGTH("upm.text.batch-nickname-length", "昵称不能超过50个字符"),
    BATCH_PHONE_INVALID("upm.text.batch-phone-invalid", "手机号码格式不正确"),
    BATCH_EMAIL_INVALID("upm.text.batch-email-invalid", "邮箱格式不正确"),
    BATCH_GENDER_INVALID("upm.text.batch-gender-invalid", "性别只能为男或女"),
    BATCH_DEPT_REQUIRED("upm.text.batch-dept-required", "所属部门不能为空"),
    BATCH_DEPT_MISSING("upm.text.batch-dept-missing", "所属部门不存在"),
    BATCH_DEPT_DISABLED("upm.text.batch-dept-disabled", "所属部门已停用"),
    BATCH_DEPT_FORBIDDEN("upm.text.batch-dept-forbidden", "无权向该部门添加用户"),
    BATCH_POSITION_MISSING("upm.text.batch-position-missing", "职位不存在"),
    BATCH_POSITION_DISABLED("upm.text.batch-position-disabled", "职位已停用"),
    BATCH_ROLE_MISSING("upm.text.batch-role-missing", "角色不存在"),
    BATCH_ROLE_DISABLED("upm.text.batch-role-disabled", "角色已停用"),

    // 租户、部门、角色、菜单及登录策略参数校验
    ROLE_ID_REQUIRED("upm.text.role-id-required", "角色id不能为空"),
    SCOPE_CHOOSE("upm.text.scope-choose", "请选择要修改的权限范围"),
    APP_ID_REQUIRED("upm.text.app-id-required", "应用id不能为空"),
    PARENT_ID_REQUIRED("upm.text.parent-id-required", "父级id不能为空"),
    DISPLAY_NAME_REQUIRED("upm.text.display-name-required", "名称不能为空"),
    DEVICE_REQUIRED("upm.text.device-required", "设备端不能为空"),
    ORG_NAME_REQUIRED("upm.text.org-name-required", "机构名称不能为空"),
    ORG_CODE_REQUIRED("upm.text.org-code-required", "机构编码不能为空"),
    STATUS_REQUIRED("upm.text.status-required", "状态不能为空"),
    LEVEL_REQUIRED("upm.text.level-required", "层级不能为空"),
    DEPT_ID_REQUIRED_UPPER("upm.text.dept-id-required-upper", "部门ID不能为空"),
    ADD_ID_FORBIDDEN("upm.text.add-id-forbidden", "新增不能填id"),
    CODE_REQUIRED("upm.text.code-required", "编码不能为空"),
    STATUS_BINARY("upm.text.status-binary", "状态值只能是1或0"),
    ROLE_DATA_SCOPE_REQUIRED("upm.text.role-data-scope-required", "数据权限类型"),
    ROLE_STATUS_BINARY("upm.text.role-status-binary", "角色状态值只能是1或0"),
    PERMISSIONS_REQUIRED("upm.text.permissions-required", "权限列表不能为空"),
    DEVICE_TYPE_REQUIRED("upm.text.device-type-required", "设备类型不能为空"),
    LOGIN_FAILURE_LIMIT_REQUIRED("upm.text.login-failure-limit-required", "最大失败次数不能为空"),
    LOGIN_FAILURE_LIMIT_MIN("upm.text.login-failure-limit-min", "最大失败次数不能小于1"),
    LOGIN_FAILURE_LIMIT_MAX("upm.text.login-failure-limit-max", "最大失败次数不能大于20"),
    LOGIN_WINDOW_REQUIRED("upm.text.login-window-required", "失败统计周期不能为空"),
    LOGIN_WINDOW_MIN("upm.text.login-window-min", "失败统计周期不能小于1分钟"),
    LOGIN_WINDOW_MAX("upm.text.login-window-max", "失败统计周期不能大于1440分钟"),
    LOGIN_LOCK_DURATION_REQUIRED("upm.text.login-lock-duration-required", "锁定时长不能为空"),
    LOGIN_LOCK_DURATION_MIN("upm.text.login-lock-duration-min", "锁定时长不能小于1分钟"),
    LOGIN_LOCK_DURATION_MAX("upm.text.login-lock-duration-max", "锁定时长不能大于1440分钟");

    private final String key;
    private final String defaultMessage;

    @Override
    public String getBasename() {
        return "i18n/upm-messages";
    }
}
