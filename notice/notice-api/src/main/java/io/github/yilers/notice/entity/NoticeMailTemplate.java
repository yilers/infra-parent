package io.github.yilers.notice.entity;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 租户邮件模板。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice_mail_template")
public class NoticeMailTemplate extends NoticeEntity {
    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "租户内有效模板编码")
    private String code;

    @Schema(description = "主题模板")
    private String subject;

    @Schema(description = "HTML、TEXT、MARKDOWN")
    private String contentType;

    @Schema(description = "模板源码")
    private String content;

    @Schema(description = "变量定义JSON")
    private String variableConfig;

    @Schema(description = "是否启用")
    private Integer usable;

    @Schema(description = "说明")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
}
