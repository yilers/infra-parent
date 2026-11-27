package io.github.yilers.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 邮件发送任务。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice_mail")
public class NoticeMail extends NoticeEntity {
    @Schema(description = "发件配置ID")
    private Long configId;

    @Schema(description = "可选来源模板ID")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long templateId;

    @Schema(description = "主题模板快照")
    private String subject;

    @Schema(description = "正文格式快照")
    private String contentType;

    @Schema(description = "正文模板快照")
    private String content;

    @Schema(description = "变量定义快照")
    private String variableConfig;

    @Schema(description = "公共参数JSON")
    private String commonParams;

    @Schema(description = "发件地址快照")
    private String fromAddress;

    @Schema(description = "发件名称快照")
    private String fromName;

    @Schema(description = "回复地址快照")
    private String replyTo;

    @Schema(description = "来源业务类型")
    private String businessType;

    @Schema(description = "来源业务ID或批次号")
    private String businessId;

    @Schema(description = "可选幂等请求号")
    private String requestId;

    @JsonIgnore
    @Schema(description = "幂等请求内容摘要")
    private String requestHash;

    @Schema(description = "发送状态")
    private String sendStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "计划发送时间")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime scheduledTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "首次开始发送时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "本轮处理完成时间")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime finishTime;

    @Schema(description = "收件人数")
    private Long recipientCount;

    @Schema(description = "SMTP接受人数")
    private Long successCount;

    @Schema(description = "最终失败人数")
    private Long failureCount;

    @Schema(description = "任务整体失败原因")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String failureReason;

    @JsonIgnore
    @Schema(description = "处理租约标识")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String leaseToken;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonIgnore
    @Schema(description = "处理租约到期时间")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime leaseUntil;
}
