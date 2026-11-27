package io.github.yilers.notice.model;

/** 通知业务状态集中定义，避免服务和任务处理器使用魔法字符串。 */
public final class NoticeTypes {
    private NoticeTypes() {}

    public enum ContentType {
        TEXT,
        HTML,
        MARKDOWN
    }

    public enum MessageType {
        ANNOUNCEMENT,
        APPROVAL,
        BUSINESS
    }

    public enum PublishStatus {
        DRAFT,
        SCHEDULED,
        PUBLISHING,
        PUBLISHED,
        FAILED
    }

    public enum MailStatus {
        DRAFT,
        SCHEDULED,
        SENDING,
        SUCCESS,
        PARTIAL_FAILED,
        FAILED
    }

    public enum DeliveryStatus {
        PENDING,
        SENDING,
        RETRY_WAIT,
        SUCCESS,
        FAILED
    }

    public enum SecurityMode {
        NONE,
        TLS,
        STARTTLS
    }

    public enum TaskKind {
        MESSAGE,
        MAIL
    }

    public enum DirectoryType {
        USER,
        DEPARTMENT,
        ROLE
    }
}
