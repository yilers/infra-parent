package io.github.yilers.shortlink.i18n;

import io.github.yilers.web.i18n.MessageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 短链模块业务提示，不依赖 UPM。 */
@Getter
@RequiredArgsConstructor
public enum ShortlinkMessage implements MessageCode {

    // 租户与号段
    TENANT_REQUIRED("shortlink.tenant-required", "缺少可信租户上下文"),
    SEGMENT_UNINITIALIZED("shortlink.segment-uninitialized", "短链号段未初始化"),
    SEGMENT_EXHAUSTED("shortlink.segment-exhausted", "短链号段已耗尽，请创建新namespace"),
    SEGMENT_CONFLICT("shortlink.segment-conflict", "号段分配冲突"),

    // 域名和配置
    TEMPLATE_CODE_REQUIRED("shortlink.template-code-required", "模板必须包含{code}"),
    URL_TEMPLATE_FORBIDDEN("shortlink.url-template-forbidden", "URL模式不需要模板"),
    CONFIG_NOT_FOUND("shortlink.config-not-found", "配置不存在"),
    CONFIG_IMMUTABLE("shortlink.config-immutable", "配置编码和目标类型不可修改"),
    DATA_CONFLICT("shortlink.data-conflict", "数据已变化，请刷新"),
    DOMAIN_FORMAT_INVALID("shortlink.domain-format-invalid", "请输入不含协议、端口和路径的合法域名"),
    DOMAIN_NOT_FOUND("shortlink.domain-not-found", "域名不存在"),
    DOMAIN_IMMUTABLE("shortlink.domain-immutable", "已有域名和协议不可修改，请新建域名"),
    PATH_RESERVED("shortlink.path-reserved", "该路径为系统保留路径"),
    CONFIG_DOMAIN_TENANT("shortlink.config-domain-tenant", "配置或域名不属于当前租户"),
    NAMESPACE_NOT_FOUND("shortlink.namespace-not-found", "namespace不存在"),
    NAMESPACE_IMMUTABLE("shortlink.namespace-immutable", "namespace标识和生成规则不可修改"),
    SHORTLINK_CONFIG_NOT_FOUND("shortlink.shortlink-config-not-found", "短链配置不存在"),
    CREDENTIAL_TENANT("shortlink.credential-tenant", "凭证不属于当前租户"),
    VERSION_CONFLICT("shortlink.version-conflict", "版本号为空或数据已变化，请刷新"),

    // 密钥
    KEY_ENCRYPT_FAILED("shortlink.key-encrypt-failed", "短码打散密钥加密失败"),
    KEY_MISSING("shortlink.key-missing", "短码打散密钥缺失，无法生成短链"),
    KEY_FORMAT_INVALID("shortlink.key-format-invalid", "短码打散密钥格式错误"),
    KEY_DECRYPT_FAILED("shortlink.key-decrypt-failed", "短码打散密钥解密失败，请检查短链主密钥"),
    MASTER_KEY_INVALID("shortlink.master-key-invalid", "SHORTLINK_MASTER_KEY必须配置为Base64编码的32字节主密钥"),

    // 短链创建与管理
    REQUEST_ID_CONFLICT("shortlink.request-id-conflict", "requestId已使用且请求参数不同"),
    REQUEST_ID_DELETED("shortlink.request-id-deleted", "requestId对应的短链已删除，请使用新的requestId"),
    AUTO_CODE_FORBIDDEN("shortlink.auto-code-forbidden", "自动模式不能指定短码"),
    CUSTOM_CODE_LENGTH("shortlink.custom-code-length", "自定义短码长度不正确"),
    CODE_DUPLICATE("shortlink.code-duplicate", "短码已使用，不能重复分配"),
    BATCH_SIZE_INVALID("shortlink.batch-size-invalid", "每批需包含1到1000条短链"),
    PERMANENT_WITH_EXPIRY("shortlink.permanent-with-expiry", "永久有效不能同时指定到期时间"),
    BATCH_ITEM_ID_INVALID("shortlink.batch-item-id-invalid", "批次内业务标识不能为空或重复"),
    BATCH_ID_CONFLICT("shortlink.batch-id-conflict", "批次标识已使用且请求内容不同"),
    LINK_NOT_FOUND("shortlink.link-not-found", "短链不存在"),
    EXPIRY_SELECTION("shortlink.expiry-selection", "请选择新的到期时间或永久有效"),
    RENEW_SHORTENS("shortlink.renew-shortens", "续期不能缩短原有效期"),
    DATA_CONFLICT_RETRY("shortlink.data-conflict-retry", "数据已变化，请刷新后重试"),
    VERSION_REQUIRED("shortlink.version-required", "版本号不能为空"),
    CONFIG_UNAVAILABLE("shortlink.config-unavailable", "短链配置不存在或已停用"),
    CREDENTIAL_NOT_GRANTED("shortlink.credential-not-granted", "调用凭证未获得该短链配置的授权"),
    NAMESPACE_UNAVAILABLE("shortlink.namespace-unavailable", "namespace不存在或已停用"),
    DOMAIN_UNAVAILABLE("shortlink.domain-unavailable", "短链域名不存在或已停用"),
    PATH_NOT_FOUND("shortlink.path-not-found", "短链路径不存在"),
    SHORT_DOMAIN_NOT_FOUND("shortlink.short-domain-not-found", "短链域名不存在"),
    URL_BUSINESS_CODE_FORBIDDEN("shortlink.url-business-code-forbidden", "URL模式不接受业务编码"),
    TEMPLATE_BUSINESS_CODE_REQUIRED("shortlink.template-business-code-required", "模板模式只接受非空业务编码"),
    EXPIRY_PAST("shortlink.expiry-past", "到期时间必须晚于当前时间"),
    REQUEST_VALIDATION_FAILED("shortlink.request-validation-failed", "请求参数校验失败"),

    // 短码与缓存
    CODE_LENGTH_INVALID("shortlink.code-length-invalid", "短码长度只支持6或8"),
    CODE_SPACE_EXHAUSTED("shortlink.code-space-exhausted", "短码号段已耗尽"),
    TARGET_URL_INVALID("shortlink.target-url-invalid", "目标地址必须是有效的HTTP或HTTPS地址，不能包含用户信息或控制字符"),
    ALGORITHM_UNSUPPORTED("shortlink.algorithm-unsupported", "短码算法未配置或不受支持"),
    PERMUTATION_KEY_LENGTH("shortlink.permutation-key-length", "短码打散密钥长度错误"),
    PERMUTATION_FAILED("shortlink.permutation-failed", "短码打散失败"),
    CACHE_BUSY("shortlink.cache-busy", "短链正在访问，请稍后重试"),
    CACHE_INTERRUPTED("shortlink.cache-interrupted", "短链修改被中断"),
    CACHE_UNAVAILABLE("shortlink.cache-unavailable", "短链缓存服务不可用，暂不能修改");

    private final String key;
    private final String defaultMessage;

    @Override
    public String getBasename() {
        return "i18n/shortlink-messages";
    }
}
