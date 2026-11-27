package io.github.yilers.notice.request;

import io.github.yilers.notice.model.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/** 邮件预览，不执行表达式请求。 */
@Schema(description = "邮件预览，不执行表达式请求。")
public record MailPreviewRequest(
        @Schema(description = "邮件主题，支持已声明的变量") String subject,
        @Schema(description = "正文格式：TEXT、HTML、MARKDOWN") String contentType,
        @Schema(description = "正文或模板源码") String content,
        @Schema(description = "变量定义，仅支持字母数字下划线，不执行表达式") List<TemplateVariable> variables,
        @Schema(description = "模板替换参数") Map<String, String> params) {}
