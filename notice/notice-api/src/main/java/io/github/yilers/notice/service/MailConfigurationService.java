package io.github.yilers.notice.service;

import io.github.yilers.notice.entity.NoticeMailConfig;
import io.github.yilers.notice.entity.NoticeMailTemplate;
import io.github.yilers.notice.request.MailConfigSaveRequest;
import io.github.yilers.notice.request.MailTemplateSaveRequest;
import io.github.yilers.notice.request.NoticeVersionRequest;

import java.util.List;

/** 租户发件配置及邮件模板管理契约。 */
public interface MailConfigurationService {
    /** 列出当前租户的发件配置，授权码字段禁止序列化。 */
    List<NoticeMailConfig> configs();

    /** 列出当前租户的邮件模板。 */
    List<NoticeMailTemplate> templates();

    /** 按业务编码获取当前租户启用的发件配置。 */
    NoticeMailConfig configByCode(String code);

    /** 按业务编码获取当前租户启用的邮件模板。 */
    NoticeMailTemplate templateByCode(String code);

    /** 按ID获取当前租户启用的发件配置。 */
    NoticeMailConfig config(Long id);

    /** 按ID获取当前租户启用的邮件模板。 */
    NoticeMailTemplate template(Long id);

    /** 保存发件配置，更新时未填写授权码则保留原密文。 */
    Long saveConfig(MailConfigSaveRequest request);

    /** 保存邮件模板，并校验正文格式及变量定义。 */
    Long saveTemplate(MailTemplateSaveRequest request);

    /** 按版本逻辑删除当前租户的发件配置。 */
    void deleteConfig(NoticeVersionRequest request);

    /** 按版本逻辑删除当前租户的邮件模板。 */
    void deleteTemplate(NoticeVersionRequest request);
}
