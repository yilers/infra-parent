package io.github.yilers.notice.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** 通知任务参数。首次启用扫描前必须执行SQL；外部调度通过 NoticeTaskDispatcher 扫描到期任务。 */
@Data
@ConfigurationProperties("notice")
public class NoticeProperties {
    /** 加密 SMTP 授权码的 Base64 主密钥。 */
    private String secretKey = "";

    /** 一个站内收件记录写入批次的最大条数。 */
    private int batchSize = 500;

    /** 单个通知或邮件允许的最大收件人数。 */
    private int maxRecipients = 100000;

    /** 每一轮邮件投递允许的累计尝试次数增量，包含首次尝试。 */
    private int maxAttempts = 3;

    /** 按尝试次数递进的重试间隔，超出列表时沿用末项。 */
    private List<Integer> retryDelaysSeconds = List.of(60, 300);

    /** 处理节点独占任务的租约时长，单位为秒。 */
    private int leaseSeconds = 120;

    /** SMTP 连接、读取及写入的超时，单位为毫秒。 */
    private int smtpTimeoutMillis = 10000;

    /** 启动时检查配置范围以及租约是否覆盖 SMTP 超时。 */
    @jakarta.annotation.PostConstruct
    public void validate() {
        if (batchSize < 1
                || batchSize > 1000
                || maxRecipients < 1
                || maxRecipients > 100000
                || maxAttempts < 1
                || maxAttempts > 10
                || leaseSeconds < 60
                || smtpTimeoutMillis < 1000
                || smtpTimeoutMillis > 20000
                || leaseSeconds * 1000L <= smtpTimeoutMillis * 3L
                || retryDelaysSeconds == null
                || retryDelaysSeconds.isEmpty()
                || retryDelaysSeconds.stream().anyMatch(delay -> delay == null || delay < 1)) {
            throw new IllegalArgumentException("notice任务参数不合法，请检查批次、租约、超时与重试配置");
        }
    }
}
