package io.github.yilers.notice.i18n;

import io.github.yilers.web.i18n.MessageCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 通知模块提示，公共校验集中管理，中英文资源与此处保持一致。 */
@Getter
@RequiredArgsConstructor
public enum NoticeMessageCode implements MessageCode {
    TENANT_REQUIRED("notice.tenant-required", "通知租户上下文不能为空"),
    LOGIN_REQUIRED("notice.login-required", "请先登录"),
    INVALID("notice.invalid", "通知参数不正确：{0}"),
    NOT_FOUND("notice.not-found", "记录不存在或无权访问"),
    CONFLICT("notice.conflict", "记录已变化，请刷新后重试"),
    STATE("notice.state", "当前状态不允许此操作"),
    CODE_EXISTS("notice.code-exists", "当前租户已存在相同编码"),
    TARGET_EMPTY("notice.target-empty", "没有匹配的有效接收人"),
    KEY_INVALID("notice.key-invalid", "notice.secret-key必须是Base64编码的32字节主密钥"),
    SECRET_FAILED("notice.secret-failed", "邮件授权码加解密失败，请检查主密钥"),
    CONFIG_UNAVAILABLE("notice.config-unavailable", "邮件发件配置不存在或已停用"),
    TEMPLATE_UNAVAILABLE("notice.template-unavailable", "邮件模板不存在或已停用"),
    VARIABLE_MISSING("notice.variable-missing", "缺少邮件模板变量：{0}"),
    FORMAT_INVALID("notice.format-invalid", "邮件模板变量或格式不正确"),
    TEST_FAILED("notice.test-failed", "测试邮件发送失败，请检查SMTP配置或服务端日志");
    private final String key;
    private final String defaultMessage;

    public String getBasename() {
        return "i18n/notice-messages";
    }
}
