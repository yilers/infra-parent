package io.github.yilers.upm.i18n;

import io.github.yilers.web.i18n.MessageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 应用、单点登录和第三方认证的业务提示。 */
@Getter
@RequiredArgsConstructor
public enum UpmAccessMessage implements MessageCode {

    // 应用管理
    APP_NOT_FOUND("upm.access.app-not-found", "应用不存在"),
    APP_UNAVAILABLE("upm.access.app-unavailable", "应用不存在或已停用"),
    APP_CODE_EXISTS("upm.access.app-code-exists", "应用编码已存在"),
    APP_CODE_IMMUTABLE("upm.access.app-code-immutable", "应用编码不可修改"),
    APP_BUILTIN_PROTECTED("upm.access.app-builtin-protected", "内置应用不可操作"),
    APP_CALLBACK_WILDCARD("upm.access.app-callback-wildcard", "回调地址不允许使用通配符"),
    APP_SECRET_REQUIRED("upm.access.app-secret-required", "请先生成SSO客户端密钥"),
    APP_CALLBACK_REQUIRED("upm.access.app-callback-required", "启用SSO前至少配置一个回调地址"),
    APP_CALLBACK_INVALID("upm.access.app-callback-invalid", "回调地址格式不正确"),
    APP_PUSH_INVALID("upm.access.app-push-invalid", "SSO推送地址格式不正确"),

    // 单点登录
    SSO_SECRET_ENCRYPT("upm.access.sso-secret-encrypt", "SSO客户端密钥加密失败"),
    SSO_SECRET_FORMAT("upm.access.sso-secret-format", "SSO客户端密钥格式错误"),
    SSO_SECRET_DECRYPT("upm.access.sso-secret-decrypt", "SSO客户端密钥解密失败，请检查主密钥配置"),
    SSO_MASTER_MISSING("upm.access.sso-master-missing", "未配置UPM_SSO_SECRET_KEY，无法使用SSO客户端密钥"),
    SSO_MASTER_BASE64("upm.access.sso-master-base64", "UPM_SSO_SECRET_KEY必须是Base64编码"),
    SSO_MASTER_LENGTH("upm.access.sso-master-length", "UPM_SSO_SECRET_KEY解码后必须为32字节"),
    SSO_TENANT_UNAVAILABLE("upm.access.sso-tenant-unavailable", "租户不存在或已停用"),
    SSO_NOT_ENABLED("upm.access.sso-not-enabled", "应用未启用单点登录"),
    SSO_CONFIG_INCOMPLETE("upm.access.sso-config-incomplete", "应用单点登录配置不完整"),
    SSO_CLIENT_REQUIRED("upm.access.sso-client-required", "client不能为空"),
    SSO_CLIENT_FORMAT("upm.access.sso-client-format", "client格式应为tenantCode:applicationCode"),
    SSO_TENANT_APP_FORBIDDEN("upm.access.sso-tenant-app-forbidden", "当前账号无权访问该租户应用"),
    SSO_CURRENT_APP_FORBIDDEN("upm.access.sso-current-app-forbidden", "当前账号没有该应用的访问权限"),
    SSO_TENANT_MISMATCH("upm.access.sso-tenant-mismatch", "用户与应用不属于同一租户"),
    SSO_USER_APP_FORBIDDEN("upm.access.sso-user-app-forbidden", "用户没有该应用的访问权限"),
    SSO_USER_UNAVAILABLE("upm.access.sso-user-unavailable", "账号不存在或已停用"),

    // 第三方认证
    THIRD_DING_TOKEN_MISSING("upm.access.third-ding-token-missing", "钉钉未返回用户访问凭证"),
    THIRD_DING_IDENTITY_MISSING("upm.access.third-ding-identity-missing", "钉钉未返回用户身份信息"),
    THIRD_DING_AUTH_FAILED("upm.access.third-ding-auth-failed", "钉钉授权失败，请重新发起绑定"),
    THIRD_PLATFORM_UNSUPPORTED("upm.access.third-platform-unsupported", "暂不支持该第三方认证平台"),
    THIRD_STATE_CREATE_FAILED("upm.access.third-state-create-failed", "生成第三方认证状态失败"),
    THIRD_BIND_STATE_MISMATCH("upm.access.third-bind-state-mismatch", "第三方认证状态与当前登录用户不匹配"),
    THIRD_ALREADY_BOUND("upm.access.third-already-bound", "该第三方账号已经被其他用户绑定"),
    THIRD_NOT_BOUND("upm.access.third-not-bound", "当前用户尚未绑定该第三方账号"),
    THIRD_PLATFORM_DISABLED("upm.access.third-platform-disabled", "当前租户尚未启用该第三方认证平台"),
    THIRD_CONFIG_INCOMPLETE("upm.access.third-config-incomplete", "第三方认证平台配置不完整"),
    THIRD_BIND_STATE_EXPIRED("upm.access.third-bind-state-expired", "第三方认证状态已过期，请重新发起绑定"),
    THIRD_BIND_STATE_INVALID("upm.access.third-bind-state-invalid", "第三方认证状态无效，请重新发起绑定"),
    THIRD_USER_SAVE_FAILED("upm.access.third-user-save-failed", "保存第三方用户信息失败"),
    THIRD_TENANT_CONTEXT_MISSING("upm.access.third-tenant-context-missing", "无法获取当前租户信息"),
    THIRD_CONFIG_EXISTS("upm.access.third-config-exists", "该第三方认证平台已经配置"),
    THIRD_PLATFORM_IMMUTABLE("upm.access.third-platform-immutable", "第三方认证平台不可修改"),
    THIRD_CONFIG_MISSING("upm.access.third-config-missing", "第三方认证配置不存在"),
    THIRD_CONFIG_BUILTIN_PROTECTED("upm.access.third-config-builtin-protected", "内置配置不可操作"),
    THIRD_CLIENT_ID_REQUIRED("upm.access.third-client-id-required", "请先配置Client ID"),
    THIRD_CLIENT_SECRET_REQUIRED("upm.access.third-client-secret-required", "请先配置Client Secret"),
    THIRD_CALLBACK_INVALID("upm.access.third-callback-invalid", "授权回调地址格式不正确"),
    THIRD_LOGIN_STATE_INVALID("upm.access.third-login-state-invalid", "第三方登录状态无效，请重新发起登录"),
    THIRD_ACCOUNT_NOT_BOUND("upm.access.third-account-not-bound", "该第三方账号尚未绑定UPM账号，请先使用账号密码登录并完成绑定"),
    THIRD_BOUND_USER_INVALID("upm.access.third-bound-user-invalid", "绑定的UPM账号不存在或租户不匹配"),
    THIRD_LOGIN_STATE_CREATE_FAILED("upm.access.third-login-state-create-failed", "生成第三方登录状态失败"),
    THIRD_LOGIN_STATE_EXPIRED("upm.access.third-login-state-expired", "第三方登录状态已过期，请重新发起登录");

    private final String key;
    private final String defaultMessage;

    @Override
    public String getBasename() {
        return "i18n/upm-messages";
    }
}
