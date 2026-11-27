package io.github.yilers.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

/** 邮件收件及发送结果。 */
@Data
@TableName("notice_mail_recipient")
public class NoticeMailRecipient {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "所属租户")
    private Long tenantId;

    @Schema(description = "邮件任务ID")
    private Long mailId;

    @Schema(description = "可选系统用户ID")
    private Long userId;

    @Schema(description = "接收邮箱")
    private String email;

    @Schema(description = "收件人名称")
    private String recipientName;

    @Schema(description = "个人变量JSON快照")
    private String params;

    @Schema(description = "接收记录发送状态")
    private String sendStatus;

    @Schema(description = "累计真实尝试次数，人工重试不清零")
    private Integer attemptCount;

    @Schema(description = "当前轮次允许的累计尝试次数上限")
    private Integer retryLimit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "下次尝试时间")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime nextRetryTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "最近尝试开始时间")
    private LocalDateTime lastAttemptTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "SMTP接受成功时间")
    private LocalDateTime sentTime;

    @Schema(description = "SMTP Message-ID")
    private String messageId;

    @Schema(description = "最近失败原因")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String failureReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
