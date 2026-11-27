package io.github.yilers.notice.task;

import io.github.yilers.notice.config.NoticeProperties;
import io.github.yilers.notice.entity.*;
import io.github.yilers.notice.mapper.NoticeTaskMapper;
import io.github.yilers.notice.model.*;
import io.github.yilers.notice.model.NoticeTypes.*;
import io.github.yilers.notice.service.*;
import io.github.yilers.notice.support.*;
import io.github.yilers.web.context.RequestContext;
import io.github.yilers.web.context.RequestContextHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;

/** 持久化任务处理入口，由调度器提交已到期任务。 多节点使用数据库条件更新抢占租约，分批事务写入；SMTP调用始终在事务外。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeTasks {
    /** 每次处理一项邮件任务时最多尝试的收件记录数。 */
    private static final int MAILS_PER_TICK = 20;

    /** 查询到期任务的位置，业务查询与更新由对应 Service 执行。 */
    private final NoticeTaskMapper tasks;

    /** 在恢复任务租户上下文后检查租户是否有效。 */
    private final NoticeDataProvider dataProvider;

    /** 站内发布的租约、收件记录和结果写入。 */
    private final MessageService messageService;

    /** 反序列化发布时保存的接收人快照。 */
    private final NoticeSupport support;

    /** 读取批次大小、租约时长与重试间隔。 */
    private final NoticeProperties properties;

    /** 读取当前可用的 SMTP 发件配置。 */
    private final MailConfigurationService configurations;

    /** 邮件投递的租约、收件记录和结果写入。 */
    private final MailService mailService;

    /** 在数据库事务外连接 SMTP 并提交邮件。 */
    private final SmtpMailSender sender;

    /** 将续租与当前批次的数据更新放入同一事务。 */
    private final TransactionTemplate transactions;

    /** 同步扫描并处理到期任务，实际异步调度由 NoticeTaskDispatcher 负责。 */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void execute() {
        LocalDateTime now = LocalDateTime.now();
        List<NoticeTaskMapper.Task> pending = new ArrayList<>(tasks.dueMessages(now));
        pending.addAll(tasks.dueMails(now));
        for (NoticeTaskMapper.Task task : pending) process(task);
    }

    /** 处理一条已定位的任务；租约在真正开始工作时抢占，不在内存排队期间提前占用。 */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void process(NoticeTaskMapper.Task task) {
        RequestContext previous = RequestContextHolder.getContext();
        try {
            RequestContextHolder.clear();
            RequestContextHolder.setTenantId(task.tenantId());
            String token = UUID.randomUUID().toString();
            if (!claim(task, token)) return;
            try {
                if (!dataProvider.isTenantAvailable()) throw new IllegalStateException("通知所属租户已停用或删除");
                if (TaskKind.MESSAGE.name().equals(task.kind())) publish(task, token);
                else send(task, token);
            } catch (LeaseLostException ignored) {
                // 租约已交给其他节点，旧处理器不得覆盖状态。
            } catch (Exception e) {
                log.error("通知任务执行失败 kind={} tenant={} id={}", task.kind(), task.tenantId(), task.id(), e);
                fail(task, token);
            }
        } finally {
            RequestContextHolder.clear();
            if (previous != null) RequestContextHolder.setContext(previous);
        }
    }

    /** 根据任务渠道尝试取得租约，并计算新的到期时间。 */
    private boolean claim(NoticeTaskMapper.Task task, String token) {
        LocalDateTime now = LocalDateTime.now(), until = now.plusSeconds(properties.getLeaseSeconds());
        if (TaskKind.MESSAGE.name().equals(task.kind()))
            return messageService.claimPublication(task.id(), token, now, until);
        return mailService.claimDelivery(task.id(), token, now, until);
    }

    /** 在写入收件记录前续租。租约已交接时终止当前处理。 */
    private void heartbeat(NoticeTaskMapper.Task task, String token) {
        LocalDateTime until = LocalDateTime.now().plusSeconds(properties.getLeaseSeconds());
        boolean renewed = TaskKind.MESSAGE.name().equals(task.kind())
                ? messageService.renewPublicationLease(task.id(), token, until)
                : mailService.renewDeliveryLease(task.id(), token, until);
        if (!renewed) throw new LeaseLostException();
    }

    /** 按冻结的接收人快照分批写入收件箱，完成后更新发布状态。 */
    private void publish(NoticeTaskMapper.Task task, String token) {
        NoticeMessage message = messageService.findForPublication(task.id());
        List<NoticeUser> users = support.list(message.getRecipientSnapshot(), NoticeUser.class);
        if (users.isEmpty()) throw new IllegalStateException("接收快照为空");
        int batchSize = properties.getBatchSize();
        for (int offset = 0; offset < users.size(); offset += batchSize) {
            List<NoticeUser> batch = users.subList(offset, Math.min(offset + batchSize, users.size()));
            transactions.executeWithoutResult(status -> {
                heartbeat(task, token);
                messageService.addPublicationRecipients(task.id(), token, batch);
            });
        }
        messageService.completePublication(task.id(), token);
    }

    /** 分批尝试投递邮件，保存每个收件人的结果与下一次重试时间。 */
    private void send(NoticeTaskMapper.Task task, String token) throws Exception {
        NoticeMail mail = mailService.detail(task.id());
        NoticeMailConfig config = configurations.config(mail.getConfigId());
        if (mail.getStartTime() == null) mailService.markDeliveryStarted(task.id(), token, LocalDateTime.now());
        List<NoticeMailRecipient> rows =
                mailService.findDueDeliveries(task.id(), LocalDateTime.now(), MAILS_PER_TICK);
        for (NoticeMailRecipient row : rows) {
            transactions.executeWithoutResult(status -> {
                heartbeat(task, token);
                row.setAttemptCount(row.getAttemptCount() + 1);
                row.setLastAttemptTime(LocalDateTime.now());
                row.setNextRetryTime(null);
                row.setSendStatus(DeliveryStatus.SENDING.name());
                row.setUpdateTime(LocalDateTime.now());
                mailService.updateDeliveryRecipient(row);
            });
            try {
                // 稳定Message-ID便于排查，不承诺SMTP严格只投递一次。
                String messageId =
                        "<notice-"
                                + row.getId()
                                + "@"
                                + mail.getFromAddress().substring(mail.getFromAddress().indexOf('@') + 1)
                                + ">";
                row.setMessageId(
                        sender.send(
                                config,
                                mail.getFromAddress(),
                                mail.getFromName(),
                                mail.getReplyTo(),
                                row.getEmail(),
                                mailService.preview(mail, row),
                                messageId));
                row.setSendStatus(DeliveryStatus.SUCCESS.name());
                row.setSentTime(LocalDateTime.now());
                row.setFailureReason(null);
            } catch (Exception e) {
                boolean permanent =
                        e instanceof org.springframework.mail.MailAuthenticationException
                                || e instanceof jakarta.mail.internet.AddressException;
                boolean retry = !permanent && row.getAttemptCount() < row.getRetryLimit();
                row.setSendStatus(retry ? DeliveryStatus.RETRY_WAIT.name() : DeliveryStatus.FAILED.name());
                if (retry) {
                    List<Integer> delays = properties.getRetryDelaysSeconds();
                    row.setNextRetryTime(
                            LocalDateTime.now()
                                    .plusSeconds(delays.get(Math.min(row.getAttemptCount() - 1, delays.size() - 1))));
                }
                // 不把可能含SMTP凭据、完整正文或连接地址的第三方异常直接返回管理页面。
                row.setFailureReason(permanent ? "SMTP_AUTH_OR_ADDRESS" : "SMTP_SEND_FAILED");
                log.warn(
                        "邮件发送失败 tenant={} mail={} recipient={} errorType={}",
                        task.tenantId(),
                        task.id(),
                        row.getId(),
                        e.getClass().getSimpleName());
            }
            transactions.executeWithoutResult(status -> {
                heartbeat(task, token);
                row.setUpdateTime(LocalDateTime.now());
                mailService.updateDeliveryRecipient(row);
            });
        }
        transactions.executeWithoutResult(status -> {
            heartbeat(task, token);
            long success = mailService.countDeliveries(task.id(), DeliveryStatus.SUCCESS);
            long failed = mailService.countDeliveries(task.id(), DeliveryStatus.FAILED);
            boolean complete = success + failed == mail.getRecipientCount();
            MailStatus state =
                    complete
                            ? failed == 0 ? MailStatus.SUCCESS : success == 0 ? MailStatus.FAILED : MailStatus.PARTIAL_FAILED
                            : MailStatus.SENDING;
            mailService.recordDeliveryProgress(task.id(), token, success, failed, state, complete);
        });
    }

    /** 记录任务失败。邮件失败时保留已经成功投递的收件记录。 */
    private void fail(NoticeTaskMapper.Task task, String token) {
        if (TaskKind.MESSAGE.name().equals(task.kind())) messageService.failPublication(task.id(), token);
        else {
            transactions.executeWithoutResult(status -> {
                heartbeat(task, token);
                mailService.failDelivery(task.id(), token);
            });
        }
    }

    /** 中断已失去租约的任务，防止旧处理器继续更新新租约持有者的数据。 */
    private static class LeaseLostException extends RuntimeException {}
}
