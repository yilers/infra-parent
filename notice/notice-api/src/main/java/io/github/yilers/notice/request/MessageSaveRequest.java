package io.github.yilers.notice.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.github.yilers.notice.model.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 站内消息草稿请求。 */
@Schema(description = "站内消息草稿请求。")
public record MessageSaveRequest(
        @Schema(description = "记录ID，新建为空") Long id,
        @Schema(description = "乐观锁版本，修改必传") Integer version,
        @Schema(description = "消息标题") String title,
        @Schema(description = "正文或模板源码") String content,
        @Schema(description = "正文格式：TEXT、HTML、MARKDOWN") String contentType,
        @Schema(description = "分类：ANNOUNCEMENT、APPROVAL、BUSINESS") String messageType,
        @Schema(description = "本租户接收范围，按用户ID去重") NoticeTarget target,
        @Schema(description = "可选站内相对路由，以单个/开头") String actionUrl,
        @Schema(description = "来源业务类型") String businessType,
        @Schema(description = "业务ID或业务批次ID，可关联多次通知") String businessId,
        @Schema(description = "可选创建幂等号，同号必须为同一请求内容") String requestId,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") @Schema(description = "计划时间，空表示立即执行；使用北京时间")
                LocalDateTime scheduledTime) {}
