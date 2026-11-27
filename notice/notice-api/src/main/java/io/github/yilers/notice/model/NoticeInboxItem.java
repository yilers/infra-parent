package io.github.yilers.notice.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 个人收件箱条目，ID为接收记录ID，不接受其他用户ID作为查询条件。 */
public record NoticeInboxItem(
        Long id,
        Long messageId,
        String title,
        String messageType,
        String content,
        String contentType,
        String actionUrl,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") LocalDateTime readTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") LocalDateTime publishTime) {}
