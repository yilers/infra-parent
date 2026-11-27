package io.github.yilers.notice.support;

import io.github.yilers.notice.config.NoticeProperties;
import io.github.yilers.notice.entity.NoticeMailConfig;
import io.github.yilers.notice.model.MailPreview;
import io.github.yilers.notice.model.NoticeTypes.SecurityMode;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** 每次按租户配置建立SMTP客户端，避免全局单例账号串租户。网络调用不持有数据库事务。 */
@Component
public class SmtpMailSender {
    private final MailSecretCipher cipher;
    private final NoticeProperties properties;

    public SmtpMailSender(MailSecretCipher cipher, NoticeProperties properties) {
        this.cipher = cipher;
        this.properties = properties;
    }

    public String send(
            NoticeMailConfig config,
            String fromAddress,
            String fromName,
            String replyTo,
            String recipient,
            MailPreview preview,
            String stableMessageId)
            throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getSmtpHost());
        sender.setPort(config.getSmtpPort());
        sender.setUsername(config.getUsername());
        sender.setPassword(cipher.decrypt(config.getPasswordCipher()));
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties options = sender.getJavaMailProperties();
        options.setProperty("mail.smtp.auth", "true");
        options.setProperty(
                "mail.smtp.ssl.enable", Boolean.toString(SecurityMode.TLS.name().equals(config.getSecurityMode())));
        boolean startTls = SecurityMode.STARTTLS.name().equals(config.getSecurityMode());
        options.setProperty("mail.smtp.starttls.enable", Boolean.toString(startTls));
        options.setProperty("mail.smtp.starttls.required", Boolean.toString(startTls));
        options.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        for (String timeout : java.util.List.of("connectiontimeout", "timeout", "writetimeout"))
            options.setProperty("mail.smtp." + timeout, Integer.toString(properties.getSmtpTimeoutMillis()));
        jakarta.mail.internet.MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(fromAddress, fromName == null ? "" : fromName);
        if (replyTo != null && !replyTo.isBlank()) helper.setReplyTo(replyTo);
        helper.setTo(recipient);
        helper.setSubject(preview.subject());
        helper.setText(preview.content(), preview.html());
        message.saveChanges();
        if (stableMessageId != null) message.setHeader("Message-ID", stableMessageId);
        sender.send(message);
        return message.getMessageID();
    }
}
