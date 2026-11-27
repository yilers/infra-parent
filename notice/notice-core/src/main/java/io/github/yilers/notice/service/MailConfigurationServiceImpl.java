package io.github.yilers.notice.service;

import static io.github.yilers.notice.support.NoticeSupport.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.notice.entity.*;
import io.github.yilers.notice.i18n.NoticeMessageCode;
import io.github.yilers.notice.mapper.*;
import io.github.yilers.notice.model.NoticeTypes.*;
import io.github.yilers.notice.request.*;
import io.github.yilers.notice.support.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 租户发件账号与模板管理。保存请求使用白名单字段，授权码绝不向列表回显。 */
@Service
@RequiredArgsConstructor
public class MailConfigurationServiceImpl implements MailConfigurationService {
    /** SMTP 发件配置持久化。 */
    private final NoticeMailConfigMapper configs;

    /** 邮件模板及变量定义持久化。 */
    private final NoticeMailTemplateMapper templates;

    /** SMTP 授权码加密工具。 */
    private final MailSecretCipher cipher;

    /** 通知模块的 JSON 序列化与参数校验工具。 */
    private final NoticeSupport support;

    /** 渲染邮件主题、正文和模板变量。 */
    private final MailRenderer renderer;

    /** 列出当前租户的发件配置，授权码字段禁止序列化。 */
    @Override
    public List<NoticeMailConfig> configs() {
        tenant();
        return configs.selectList(
                Wrappers.<NoticeMailConfig>lambdaQuery().orderByDesc(NoticeMailConfig::getCreateTime));
    }

    /** 列出当前租户的邮件模板。 */
    @Override
    public List<NoticeMailTemplate> templates() {
        tenant();
        return templates.selectList(
                Wrappers.<NoticeMailTemplate>lambdaQuery().orderByDesc(NoticeMailTemplate::getCreateTime));
    }

    /** 按业务编码获取当前租户启用的发件配置。 */
    @Override
    public NoticeMailConfig configByCode(String code) {
        tenant();
        var value =
                configs.selectOne(
                        Wrappers.<NoticeMailConfig>lambdaQuery()
                                .eq(NoticeMailConfig::getCode, text(code, 100, "configCode")));
        require(value != null && CommonConst.YES.equals(value.getUsable()), NoticeMessageCode.CONFIG_UNAVAILABLE);
        return value;
    }

    /** 按业务编码获取当前租户启用的邮件模板。 */
    @Override
    public NoticeMailTemplate templateByCode(String code) {
        tenant();
        var value =
                templates.selectOne(
                        Wrappers.<NoticeMailTemplate>lambdaQuery()
                                .eq(NoticeMailTemplate::getCode, text(code, 100, "templateCode")));
        require(value != null && CommonConst.YES.equals(value.getUsable()), NoticeMessageCode.TEMPLATE_UNAVAILABLE);
        return value;
    }

    /** 按ID获取当前租户启用的发件配置。 */
    @Override
    public NoticeMailConfig config(Long id) {
        tenant();
        NoticeMailConfig config = configs.selectById(id);
        require(config != null && CommonConst.YES.equals(config.getUsable()), NoticeMessageCode.CONFIG_UNAVAILABLE);
        return config;
    }

    /** 按ID获取当前租户启用的邮件模板。 */
    @Override
    public NoticeMailTemplate template(Long id) {
        tenant();
        NoticeMailTemplate template = templates.selectById(id);
        require(
                template != null && CommonConst.YES.equals(template.getUsable()),
                NoticeMessageCode.TEMPLATE_UNAVAILABLE);
        return template;
    }

    /** 保存发件配置，更新时未填写授权码则保留原密文。 */
    @Override
    @Transactional
    public Long saveConfig(MailConfigSaveRequest request) {
        tenant();
        NoticeMailConfig entity;
        if (request.id() == null) {
            entity = new NoticeMailConfig();
            initialize(entity);
        } else {
            entity = configs.selectById(request.id());
            version(entity, request.version());
        }
        entity.setName(text(request.name(), 100, "name"));
        entity.setCode(code(request.code()));
        require(
                configs.selectCount(
                                Wrappers.<NoticeMailConfig>lambdaQuery()
                                        .eq(NoticeMailConfig::getCode, entity.getCode())
                                        .ne(NoticeMailConfig::getId, entity.getId()))
                        == 0,
                NoticeMessageCode.CODE_EXISTS);
        entity.setSmtpHost(text(request.smtpHost(), 255, "smtpHost"));
        require(!entity.getSmtpHost().matches(".*[\s/].*"), NoticeMessageCode.INVALID, "smtpHost");
        require(
                request.smtpPort() != null && request.smtpPort() > 0 && request.smtpPort() <= 65535,
                NoticeMessageCode.INVALID,
                "smtpPort");
        entity.setSmtpPort(request.smtpPort());
        entity.setSecurityMode(type(SecurityMode.class, request.securityMode()).name());
        entity.setUsername(text(request.username(), 255, "username"));
        if (request.password() != null && !request.password().isEmpty()) {
            require(request.password().length() <= 4096, NoticeMessageCode.INVALID, "password");
            entity.setPasswordCipher(cipher.encrypt(request.password()));
        }
        require(entity.getPasswordCipher() != null, NoticeMessageCode.INVALID, "password");
        entity.setFromAddress(email(request.fromAddress()));
        entity.setFromName(optional(request.fromName(), 100, "fromName"));
        entity.setReplyTo(request.replyTo() == null || request.replyTo().isBlank() ? null : email(request.replyTo()));
        entity.setUsable(flag(request.usable()));
        entity.setDescription(optional(request.description(), 500, "description"));
        entity.setUpdateTime(LocalDateTime.now());
        require(
                (request.id() == null ? configs.insert(entity) : configs.updateById(entity)) == 1,
                NoticeMessageCode.CONFLICT);
        return entity.getId();
    }

    /** 保存邮件模板，并校验正文格式及变量定义。 */
    @Override
    @Transactional
    public Long saveTemplate(MailTemplateSaveRequest request) {
        tenant();
        NoticeMailTemplate entity;
        if (request.id() == null) {
            entity = new NoticeMailTemplate();
            initialize(entity);
        } else {
            entity = templates.selectById(request.id());
            version(entity, request.version());
        }
        entity.setName(text(request.name(), 100, "name"));
        entity.setCode(code(request.code()));
        require(
                templates.selectCount(
                                Wrappers.<NoticeMailTemplate>lambdaQuery()
                                        .eq(NoticeMailTemplate::getCode, entity.getCode())
                                        .ne(NoticeMailTemplate::getId, entity.getId()))
                        == 0,
                NoticeMessageCode.CODE_EXISTS);
        var variables =
                request.variables() == null
                        ? List.<io.github.yilers.notice.model.TemplateVariable>of()
                        : request.variables();
        java.util.Map<String, String> examples = new java.util.LinkedHashMap<>();
        variables.forEach(v -> {
            require(v != null && v.name() != null, NoticeMessageCode.FORMAT_INVALID);
            examples.put(v.name(), v.example() == null || v.example().isBlank() ? "example" : v.example());
        });
        renderer.render(
                new MailPreviewRequest(
                        request.subject(), request.contentType(), request.content(), variables, examples));
        entity.setSubject(request.subject());
        entity.setContentType(request.contentType());
        entity.setContent(request.content());
        entity.setVariableConfig(support.json(variables));
        entity.setUsable(flag(request.usable()));
        entity.setDescription(optional(request.description(), 500, "description"));
        entity.setUpdateTime(LocalDateTime.now());
        require(
                (request.id() == null ? templates.insert(entity) : templates.updateById(entity)) == 1,
                NoticeMessageCode.CONFLICT);
        return entity.getId();
    }

    /** 按版本逻辑删除当前租户的发件配置。 */
    @Override
    @Transactional
    public void deleteConfig(NoticeVersionRequest request) {
        NoticeMailConfig entity = configs.selectById(request.id());
        version(entity, request.version());
        require(
                configs.update(
                                null,
                                Wrappers.<NoticeMailConfig>lambdaUpdate()
                                        .eq(NoticeMailConfig::getId, entity.getId())
                                        .eq(NoticeMailConfig::getVersion, request.version())
                                        .set(NoticeMailConfig::getDeleted, 1)
                                        .setSql("version = version + 1"))
                        == 1,
                NoticeMessageCode.CONFLICT);
    }

    /** 按版本逻辑删除当前租户的邮件模板。 */
    @Override
    @Transactional
    public void deleteTemplate(NoticeVersionRequest request) {
        NoticeMailTemplate entity = templates.selectById(request.id());
        version(entity, request.version());
        require(
                templates.update(
                                null,
                                Wrappers.<NoticeMailTemplate>lambdaUpdate()
                                        .eq(NoticeMailTemplate::getId, entity.getId())
                                        .eq(NoticeMailTemplate::getVersion, request.version())
                                        .set(NoticeMailTemplate::getDeleted, 1)
                                        .setSql("version = version + 1"))
                        == 1,
                NoticeMessageCode.CONFLICT);
    }

    /** 校验启用状态只取系统约定值。 */
    private int flag(Integer value) {
        require(CommonConst.YES.equals(value) || CommonConst.NO.equals(value), NoticeMessageCode.INVALID, "usable");
        return value;
    }

    /** 校验可供业务调用的配置或模板编码。 */
    private String code(String value) {
        require(value != null && value.matches("[A-Za-z][A-Za-z0-9_.:-]{0,99}"), NoticeMessageCode.INVALID, "code");
        return value;
    }
}
