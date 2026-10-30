package io.github.yilers.upm.i18n;

import io.github.yilers.web.i18n.MessageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 职位、设备和系统调用凭证提示。 */
@Getter
@RequiredArgsConstructor
public enum UpmExtraMessage implements MessageCode {

    POSITION_CODE_EXISTS("upm.extra.position-code-exists", "职位编码已存在"),
    POSITION_NOT_FOUND("upm.extra.position-not-found", "职位不存在"),
    POSITION_HAS_USERS("upm.extra.position-has-users", "职位下存在用户"),
    DEVICE_VERSION_REQUIRED("upm.extra.device-version-required", "版本号不能为空"),
    NO_PERMISSION("upm.extra.no-permission", "没有权限"),
    API_SECRET_ENCRYPT("upm.extra.api-secret-encrypt", "系统调用密钥加密失败"),
    API_SECRET_FORMAT("upm.extra.api-secret-format", "系统调用密钥格式错误"),
    API_SECRET_DECRYPT("upm.extra.api-secret-decrypt", "系统调用密钥解密失败，请检查主密钥配置"),
    API_MASTER_INVALID("upm.extra.api-master-invalid", "upm.api.secret-key必须配置为Base64编码的32字节随机主密钥"),
    API_CREDENTIAL_NOT_FOUND("upm.extra.api-credential-not-found", "调用凭证不存在"),
    API_ID_VERSION_REQUIRED("upm.extra.api-id-version-required", "ID和版本号不能为空"),
    API_CONFLICT("upm.extra.api-conflict", "数据已变化，请刷新"),
    API_EXPIRATION_INVALID("upm.extra.api-expiration-invalid", "凭证失效时间必须晚于当前时间"),
    API_PERMISSION_UNKNOWN("upm.extra.api-permission-unknown", "包含未注册的接口权限"),
    API_TENANT_REQUIRED("upm.extra.api-tenant-required", "缺少可信租户上下文"),
    DATA_UPDATE_CONFLICT("upm.extra.data-update-conflict", "更新失败 数据已经变更");

    private final String key;
    private final String defaultMessage;

    @Override
    public String getBasename() {
        return "i18n/upm-messages";
    }
}
