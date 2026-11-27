package io.github.yilers.notice.request;

import io.github.yilers.notice.model.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 邮件模板请求。 */
@Schema(description = "邮件模板请求。")
public record MailTemplateSaveRequest(
        @Schema(description = "记录ID，新建为空") Long id,
        @Schema(description = "乐观锁版本，修改必传") Integer version,
        @Schema(description = "配置或模板名称") String name,
        @Schema(description = "同租户有效记录内唯一编码") String code,
        @Schema(description = "邮件主题，支持已声明的变量") String subject,
        @Schema(description = "正文格式：TEXT、HTML、MARKDOWN") String contentType,
        @Schema(description = "正文或模板源码") String content,
        @Schema(description = "变量定义，仅支持字母数字下划线，不执行表达式") List<TemplateVariable> variables,
        @Schema(description = "启用状态：1启用，0停用") Integer usable,
        @Schema(description = "说明") String description) {}
