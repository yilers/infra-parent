package io.github.yilers.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 站内消息。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice_message")
public class NoticeMessage extends NoticeEntity {
    @TableField(exist = false)
    @Schema(description = "已读人数，分页列表批量统计")
    private Long readCount;

    @TableField(exist = false)
    @Schema(description = "未读人数，发布成功后有意义")
    private Long unreadCount;

    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "管理详情中 Markdown 正文渲染出的 HTML")
    private String renderedContent;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "TEXT、HTML、MARKDOWN")
    private String contentType;

    @Schema(description = "ANNOUNCEMENT、APPROVAL、BUSINESS")
    private String messageType;

    @Schema(description = "来源业务类型")
    private String businessType;

    @Schema(description = "来源业务ID或批次号")
    private String businessId;

    @Schema(description = "可选幂等请求号")
    private String requestId;

    @JsonIgnore
    @Schema(description = "幂等请求内容摘要")
    private String requestHash;

    @Schema(description = "接收范围JSON")
    private String targetConfig;

    @JsonIgnore
    @TableField(select = false)
    @Schema(description = "发布时冻结的接收人快照，不向列表输出")
    private String recipientSnapshot;

    @Schema(description = "站内相对跳转路径")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String actionUrl;

    @Schema(description = "发布状态")
    private String publishStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "计划发布时间")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime scheduledTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "发布完成时间")
    private LocalDateTime publishTime;

    @Schema(description = "已生成接收记录数")
    private Long recipientCount;

    @Schema(description = "发布失败原因")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String failureReason;

    @JsonIgnore
    @Schema(description = "任务处理租约标识")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String leaseToken;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonIgnore
    @Schema(description = "任务租约到期时间")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime leaseUntil;
}
