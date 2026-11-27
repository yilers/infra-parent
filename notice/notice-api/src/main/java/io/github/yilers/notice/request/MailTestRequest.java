package io.github.yilers.notice.request;

import io.github.yilers.notice.model.*;
import io.swagger.v3.oas.annotations.media.Schema;

/** 使用已保存配置发送测试邮件请求。 */
@Schema(description = "使用已保存配置发送测试邮件请求。")
public record MailTestRequest(
        @Schema(description = "当前租户启用的SMTP配置ID") Long configId,
        @Schema(description = "测试收件邮箱") String email,
        @Schema(description = "测试邮件正文和参数") MailPreviewRequest preview) {}
