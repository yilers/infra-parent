package io.github.yilers.notice.request;

import io.github.yilers.notice.model.*;
import io.swagger.v3.oas.annotations.media.Schema;

/** 租户发件配置，密码为空保留原值请求。 */
@Schema(description = "租户发件配置，密码为空保留原值请求。")
public record MailConfigSaveRequest(
        @Schema(description = "记录ID，新建为空") Long id,
        @Schema(description = "乐观锁版本，修改必传") Integer version,
        @Schema(description = "配置或模板名称") String name,
        @Schema(description = "同租户有效记录内唯一编码") String code,
        @Schema(description = "SMTP服务器地址，不带协议") String smtpHost,
        @Schema(description = "SMTP端口，1至65535") Integer smtpPort,
        @Schema(description = "连接方式：NONE、TLS、STARTTLS") String securityMode,
        @Schema(description = "SMTP登录账号") String username,
        @Schema(description = "SMTP授权码，新建必填，编辑为空保留") String password,
        @Schema(description = "发件邮箱") String fromAddress,
        @Schema(description = "发件显示名称") String fromName,
        @Schema(description = "可选回复邮箱") String replyTo,
        @Schema(description = "启用状态：1启用，0停用") Integer usable,
        @Schema(description = "说明") String description) {}
