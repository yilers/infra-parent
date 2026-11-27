package io.github.yilers.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 租户SMTP发件配置。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice_mail_config")
public class NoticeMailConfig extends NoticeEntity {
    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "租户内有效配置编码")
    private String code;

    @Schema(description = "SMTP地址")
    private String smtpHost;

    @Schema(description = "SMTP端口")
    private Integer smtpPort;

    @Schema(description = "NONE、TLS、STARTTLS")
    private String securityMode;

    @Schema(description = "SMTP用户名")
    private String username;

    @JsonIgnore
    @Schema(description = "SMTP授权码密文，禁止回显")
    @lombok.ToString.Exclude
    private String passwordCipher;

    @Schema(description = "发件邮箱")
    private String fromAddress;

    @Schema(description = "发件显示名称")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fromName;

    @Schema(description = "回复邮箱")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String replyTo;

    @Schema(description = "是否启用")
    private Integer usable;

    @Schema(description = "说明")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
}
