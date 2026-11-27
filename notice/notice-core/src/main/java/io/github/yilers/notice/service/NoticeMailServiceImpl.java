package io.github.yilers.notice.service;

import static io.github.yilers.notice.support.NoticeSupport.*;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.yilers.notice.config.NoticeProperties;
import io.github.yilers.notice.entity.*;
import io.github.yilers.notice.i18n.NoticeMessageCode;
import io.github.yilers.notice.mapper.*;
import io.github.yilers.notice.model.*;
import io.github.yilers.notice.model.NoticeTypes.*;
import io.github.yilers.notice.request.*;
import io.github.yilers.notice.support.*;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** 邮件任务保存及发布；正文和变量定义冻结为快照，模板后续修改不影响已提交任务。 */
@Service
@RequiredArgsConstructor
public class NoticeMailServiceImpl implements MailService {
    /** 邮件任务及投递汇总持久化。 */
    private final NoticeMailMapper mails;

    /** 接收记录持久化。 */
    private final NoticeMailRecipientMapper recipients;

    /** 加载租户可用的发件配置和模板。 */
    private final MailConfigurationService configurations;

    /** 从租户组织目录解析接收范围。 */
    private final NoticeAudienceService audience;

    /** 宿主提供的用户、部门和角色基础查询。 */
    private final NoticeDataProvider dataProvider;

    /** 通知模块的 JSON 序列化与参数校验工具。 */
    private final NoticeSupport support;

    /** 渲染邮件主题、正文和模板变量。 */
    private final MailRenderer renderer;

    /** 通知模块的收件人数与重试等运行配置。 */
    private final NoticeProperties properties;

    /** 发布提交事件，由事务提交后的监听器触发后台处理。 */
    private final ApplicationEventPublisher events;

    /** 分页查询当前租户的邮件任务。 */
    @Override
    public NoticePage<NoticeMail> page(String keyword, int current, int size) {
        tenant();
        NoticeSupport.page(current, size);
        var result =
                mails.selectPage(
                        new Page<>(current, size),
                        Wrappers.<NoticeMail>lambdaQuery()
                                .like(keyword != null && !keyword.isBlank(), NoticeMail::getSubject, keyword)
                                .orderByDesc(NoticeMail::getCreateTime)
                                .orderByDesc(NoticeMail::getId));
        return new NoticePage<>(result.getTotal(), result.getRecords());
    }

    /** 按ID查询当前租户的邮件任务。 */
    @Override
    public NoticeMail detail(Long id) {
        tenant();
        var entity = mails.selectById(id);
        require(entity != null, NoticeMessageCode.NOT_FOUND);
        return entity;
    }

    /** 分页查询邮件收件人及其投递结果。 */
    @Override
    public NoticePage<NoticeMailRecipient> recipients(Long id, int current, int size) {
        detail(id);
        NoticeSupport.page(current, size);
        var result =
                recipients.selectPage(
                        new Page<>(current, size),
                        Wrappers.<NoticeMailRecipient>lambdaQuery()
                                .eq(NoticeMailRecipient::getMailId, id)
                                .orderByAsc(NoticeMailRecipient::getId));
        return new NoticePage<>(result.getTotal(), result.getRecords());
    }

    /** 按编码读取发件配置和模板，提交邮件发送。 */
    @Override
    @Transactional
    public Long sendTemplate(MailTemplateSendRequest request) {
        var config = configurations.configByCode(request.configCode());
        var template = configurations.templateByCode(request.templateCode());
        return submit(
                new MailSaveRequest(
                        null,
                        null,
                        config.getId(),
                        template.getId(),
                        null,
                        null,
                        null,
                        null,
                        request.params(),
                        request.recipients(),
                        request.target(),
                        request.businessType(),
                        request.businessId(),
                        request.requestId(),
                        request.scheduledTime()));
    }

    /** 保存并提交邮件，重复请求复用原任务。 */
    @Override
    @Transactional
    public Long submit(MailSaveRequest request) {
        Long id = save(request);
        NoticeMail entity = detail(id);
        if (MailStatus.DRAFT.name().equals(entity.getSendStatus()))
            send(new NoticeVersionRequest(id, entity.getVersion()));
        return id;
    }

    /** 保存邮件草稿，冻结正文、参数和去重后的收件人。 */
    @Override
    @Transactional
    public Long save(MailSaveRequest request) {
        tenant();
        String requestHash = support.fingerprint(request);
        if (request.id() == null && request.requestId() != null && !request.requestId().isBlank()) {
            text(request.requestId(), 128, "requestId");
            var existing =
                    mails.selectOne(
                            Wrappers.<NoticeMail>lambdaQuery().eq(NoticeMail::getRequestId, request.requestId()));
            if (existing != null) {
                require(requestHash.equals(existing.getRequestHash()), NoticeMessageCode.CONFLICT);
                return existing.getId();
            }
        }
        NoticeMail entity;
        if (request.id() == null) {
            entity = new NoticeMail();
            initialize(entity);
            entity.setSendStatus(MailStatus.DRAFT.name());
        } else {
            entity = detail(request.id());
            version(entity, request.version());
            require(MailStatus.DRAFT.name().equals(entity.getSendStatus()), NoticeMessageCode.STATE);
        }
        if (request.id() != null)
            require(
                    java.util.Objects.equals(
                            entity.getRequestId(),
                            request.requestId() == null || request.requestId().isBlank() ? null : request.requestId()),
                    NoticeMessageCode.INVALID,
                    "requestId immutable");
        var config = configurations.config(request.configId());
        entity.setConfigId(config.getId());
        entity.setFromAddress(config.getFromAddress());
        entity.setFromName(config.getFromName());
        entity.setReplyTo(config.getReplyTo());
        if (request.templateId() != null) {
            var template = configurations.template(request.templateId());
            entity.setTemplateId(template.getId());
            entity.setSubject(template.getSubject());
            entity.setContentType(template.getContentType());
            entity.setContent(template.getContent());
            entity.setVariableConfig(template.getVariableConfig());
        } else {
            entity.setTemplateId(null);
            entity.setSubject(text(request.subject(), 500, "subject"));
            entity.setContentType(type(ContentType.class, request.contentType()).name());
            entity.setContent(text(request.content(), 600000, "content"));
            entity.setVariableConfig(support.json(request.variables() == null ? List.of() : request.variables()));
        }
        entity.setCommonParams(support.json(request.commonParams() == null ? Map.of() : request.commonParams()));
        entity.setBusinessType(optional(request.businessType(), 100, "businessType"));
        entity.setBusinessId(optional(request.businessId(), 128, "businessId"));
        entity.setRequestId(
                request.requestId() == null || request.requestId().isBlank()
                        ? null
                        : text(request.requestId(), 128, "requestId"));
        entity.setRequestHash(requestHash);
        entity.setScheduledTime(request.scheduledTime());
        entity.setUpdateTime(LocalDateTime.now());
        // 编辑草稿未提交新接收范围时保留原快照，不能用前端当前页的接收人覆盖整批。
        if (request.id() != null && request.recipients() == null && request.target() == null) {
            int pageNumber = 1;
            while (true) {
                var batch =
                        recipients.selectPage(
                                new Page<>(pageNumber++, properties.getBatchSize()),
                                Wrappers.<NoticeMailRecipient>lambdaQuery()
                                        .eq(NoticeMailRecipient::getMailId, entity.getId())
                                        .orderByAsc(NoticeMailRecipient::getId));
                batch.getRecords().forEach(row -> preview(entity, row));
                if (!batch.hasNext()) break;
            }
            require(mails.updateById(entity) == 1, NoticeMessageCode.CONFLICT);
            return entity.getId();
        }
        List<MailReceiver> receivers = new ArrayList<>(request.recipients() == null ? List.of() : request.recipients());
        if (request.target() != null)
            for (NoticeUser user : audience.resolve(request.target()))
                receivers.add(new MailReceiver(user.id(), user.email(), user.name(), Map.of()));
        require(
                !receivers.isEmpty() && receivers.size() <= properties.getMaxRecipients(),
                NoticeMessageCode.TARGET_EMPTY);
        // 外部邮箱可不关联用户；传入userId时必须属于当前租户，禁止伪造跨租户关联。
        var userIds = receivers.stream().map(MailReceiver::userId).filter(Objects::nonNull).distinct().toList();
        Set<Long> validIds =
                userIds.isEmpty()
                        ? Set.of()
                        : new HashSet<>(dataProvider.findUsersByIds(userIds).stream().map(NoticeUser::id).toList());
        Map<String, MailReceiver> unique = new LinkedHashMap<>();
        for (MailReceiver receiver : receivers) {
            require(
                    receiver != null && (receiver.userId() == null || validIds.contains(receiver.userId())),
                    NoticeMessageCode.INVALID,
                    "userId");
            String email = NoticeSupport.email(receiver.email());
            MailReceiver previous = unique.putIfAbsent(email.toLowerCase(Locale.ROOT), receiver);
            if (previous != null) {
                require(
                        java.util.Objects.equals(
                                previous.params() == null ? Map.of() : previous.params(),
                                receiver.params() == null ? Map.of() : receiver.params()),
                        NoticeMessageCode.INVALID,
                        "duplicate email parameters");
            }
        }
        entity.setRecipientCount((long) unique.size());
        entity.setSuccessCount(0L);
        entity.setFailureCount(0L);
        require(
                (request.id() == null ? mails.insert(entity) : mails.updateById(entity)) == 1,
                NoticeMessageCode.CONFLICT);
        if (request.id() != null)
            recipients.delete(
                    Wrappers.<NoticeMailRecipient>lambdaQuery().eq(NoticeMailRecipient::getMailId, entity.getId()));
        List<NoticeMailRecipient> records = new ArrayList<>();
        for (MailReceiver receiver : unique.values()) {
            NoticeMailRecipient row = new NoticeMailRecipient();
            row.setId(IdWorker.getId());
            row.setTenantId(tenant());
            row.setMailId(entity.getId());
            row.setUserId(receiver.userId());
            row.setEmail(receiver.email());
            row.setRecipientName(optional(receiver.name(), 100, "recipientName"));
            row.setParams(support.json(receiver.params() == null ? Map.of() : receiver.params()));
            row.setSendStatus(DeliveryStatus.PENDING.name());
            row.setAttemptCount(0);
            row.setRetryLimit(properties.getMaxAttempts());
            row.setCreateTime(LocalDateTime.now());
            row.setUpdateTime(row.getCreateTime());
            preview(entity, row);
            records.add(row);
        }
        for (int offset = 0; offset < records.size(); offset += properties.getBatchSize())
            recipients.insertBatch(
                    records.subList(offset, Math.min(offset + properties.getBatchSize(), records.size())));
        return entity.getId();
    }

    /** 预览指定邮件任务，优先使用首个收件人的变量。 */
    @Override
    public MailPreview preview(Long id) {
        NoticeMail mail = detail(id);
        NoticeMailRecipient receiver =
                recipients.selectOne(
                        Wrappers.<NoticeMailRecipient>lambdaQuery()
                                .eq(NoticeMailRecipient::getMailId, id)
                                .orderByAsc(NoticeMailRecipient::getId)
                                .last("LIMIT 1"));
        require(receiver != null, NoticeMessageCode.TARGET_EMPTY);
        return preview(mail, receiver);
    }

    /** 使用任务快照及收件人变量生成邮件内容。 */
    @Override
    public MailPreview preview(NoticeMail mail, NoticeMailRecipient receiver) {
        Map<String, String> values = new LinkedHashMap<>(support.params(mail.getCommonParams()));
        values.putAll(support.params(receiver.getParams()));
        return renderer.render(
                new MailPreviewRequest(
                        mail.getSubject(),
                        mail.getContentType(),
                        mail.getContent(),
                        support.list(mail.getVariableConfig(), TemplateVariable.class),
                        values));
    }

    /** 提交草稿。已到期的邮件在事务提交后触发。 */
    @Override
    @Transactional
    public void send(NoticeVersionRequest request) {
        var entity = detail(request.id());
        version(entity, request.version());
        require(MailStatus.DRAFT.name().equals(entity.getSendStatus()), NoticeMessageCode.STATE);
        configurations.config(entity.getConfigId());
        entity.setSendStatus(MailStatus.SCHEDULED.name());
        entity.setUpdateTime(LocalDateTime.now());
        require(mails.updateById(entity) == 1, NoticeMessageCode.CONFLICT);
        // 到期任务在事务提交后尝试执行，未来任务由调度器到点扫描。
        if (entity.getScheduledTime() == null || !entity.getScheduledTime().isAfter(LocalDateTime.now()))
            events.publishEvent(new NoticeTaskMapper.Task(entity.getId(), tenant(), TaskKind.MAIL.name()));
    }

    /** 恢复失败的收件记录并提交重试，保留累计尝试次数。 */
    @Override
    @Transactional
    public void retry(NoticeVersionRequest request) {
        var entity = detail(request.id());
        version(entity, request.version());
        require(
                Set.of(MailStatus.FAILED.name(), MailStatus.PARTIAL_FAILED.name()).contains(entity.getSendStatus()),
                NoticeMessageCode.STATE);
        require(
                recipients.selectCount(
                                Wrappers.<NoticeMailRecipient>lambdaQuery()
                                        .eq(NoticeMailRecipient::getMailId, entity.getId())
                                        .eq(NoticeMailRecipient::getSendStatus, DeliveryStatus.FAILED.name()))
                        > 0,
                NoticeMessageCode.STATE);
        configurations.config(entity.getConfigId());
        recipients.update(
                null,
                Wrappers.<NoticeMailRecipient>lambdaUpdate()
                        .eq(NoticeMailRecipient::getMailId, entity.getId())
                        .eq(NoticeMailRecipient::getSendStatus, DeliveryStatus.FAILED.name())
                        .set(NoticeMailRecipient::getSendStatus, DeliveryStatus.PENDING.name())
                        .setSql("retry_limit = attempt_count + " + Math.max(1, properties.getMaxAttempts()))
                        .set(NoticeMailRecipient::getNextRetryTime, null)
                        .set(NoticeMailRecipient::getUpdateTime, LocalDateTime.now()));
        entity.setSendStatus(MailStatus.SCHEDULED.name());
        entity.setScheduledTime(null);
        entity.setFinishTime(null);
        entity.setFailureReason(null);
        require(mails.updateById(entity) == 1, NoticeMessageCode.CONFLICT);
        events.publishEvent(new NoticeTaskMapper.Task(entity.getId(), tenant(), TaskKind.MAIL.name()));
    }

    /** 按版本删除邮件草稿。 */
    @Override
    @Transactional
    public void delete(NoticeVersionRequest request) {
        var entity = detail(request.id());
        version(entity, request.version());
        require(MailStatus.DRAFT.name().equals(entity.getSendStatus()), NoticeMessageCode.STATE);
        require(
                mails.update(
                                null,
                                Wrappers.<NoticeMail>lambdaUpdate()
                                        .eq(NoticeMail::getId, entity.getId())
                                        .eq(NoticeMail::getVersion, request.version())
                                        .set(NoticeMail::getDeleted, 1))
                        == 1,
                NoticeMessageCode.CONFLICT);
    }

    /** 后台抢占只处理已到期且租约可用的邮件。 */
    @Override
    public boolean claimDelivery(Long id, String token, LocalDateTime now, LocalDateTime until) {
        return mails.update(
                        null,
                        Wrappers.<NoticeMail>lambdaUpdate()
                                .eq(NoticeMail::getId, id)
                                .in(NoticeMail::getSendStatus, MailStatus.SCHEDULED.name(), MailStatus.SENDING.name())
                                .and(q ->
                                        q.isNull(NoticeMail::getScheduledTime)
                                                .or()
                                                .le(NoticeMail::getScheduledTime, now))
                                .and(q -> q.isNull(NoticeMail::getLeaseUntil).or().lt(NoticeMail::getLeaseUntil, now))
                                .set(NoticeMail::getLeaseToken, token)
                                .set(NoticeMail::getLeaseUntil, until)
                                .set(NoticeMail::getSendStatus, MailStatus.SENDING.name()))
                == 1;
    }

    /** 仅允许持有当前租约的任务延长到期时间。 */
    @Override
    public boolean renewDeliveryLease(Long id, String token, LocalDateTime until) {
        return mails.update(null, leasedDelivery(id, token).set(NoticeMail::getLeaseUntil, until)) == 1;
    }

    /** 记录邮件任务首次开始投递的时间。 */
    @Override
    public void markDeliveryStarted(Long id, String token, LocalDateTime now) {
        mails.update(null, leasedDelivery(id, token).set(NoticeMail::getStartTime, now));
    }

    /** 按收件记录ID顺序读取本轮可投递的记录，包含到期重试及中断恢复。 */
    @Override
    public List<NoticeMailRecipient> findDueDeliveries(Long id, LocalDateTime now, int limit) {
        return recipients.selectList(
                Wrappers.<NoticeMailRecipient>lambdaQuery()
                        .eq(NoticeMailRecipient::getMailId, id)
                        .in(
                                NoticeMailRecipient::getSendStatus,
                                DeliveryStatus.PENDING.name(),
                                DeliveryStatus.RETRY_WAIT.name(),
                                DeliveryStatus.SENDING.name())
                        .and(q ->
                                q.isNull(NoticeMailRecipient::getNextRetryTime)
                                        .or()
                                        .le(NoticeMailRecipient::getNextRetryTime, now))
                        .orderByAsc(NoticeMailRecipient::getId)
                        .last("LIMIT " + limit));
    }

    /** 在任务续租的事务内保存单个收件人的尝试状态和结果。 */
    @Override
    public void updateDeliveryRecipient(NoticeMailRecipient recipient) {
        recipients.updateById(recipient);
    }

    /** 按状态统计邮件的收件记录数。 */
    @Override
    public long countDeliveries(Long mailId, DeliveryStatus state) {
        return recipients.selectCount(
                Wrappers.<NoticeMailRecipient>lambdaQuery()
                        .eq(NoticeMailRecipient::getMailId, mailId)
                        .eq(NoticeMailRecipient::getSendStatus, state.name()));
    }

    /** 写入邮件汇总进度并释放租约，全部完成时记录完成时间。 */
    @Override
    public void recordDeliveryProgress(
            Long id, String token, long success, long failed, MailStatus state, boolean complete) {
        mails.update(
                null,
                leasedDelivery(id, token)
                        .set(NoticeMail::getSuccessCount, success)
                        .set(NoticeMail::getFailureCount, failed)
                        .set(NoticeMail::getSendStatus, state.name())
                        .set(complete, NoticeMail::getFinishTime, LocalDateTime.now())
                        .set(NoticeMail::getLeaseToken, null)
                        .set(NoticeMail::getLeaseUntil, null)
                        .set(NoticeMail::getUpdateTime, LocalDateTime.now()));
    }

    /** 由任务在续租的同一事务中调用，保留已成功的收件记录。 */
    @Override
    public void failDelivery(Long id, String token) {
        recipients.update(
                null,
                Wrappers.<NoticeMailRecipient>lambdaUpdate()
                        .eq(NoticeMailRecipient::getMailId, id)
                        .ne(NoticeMailRecipient::getSendStatus, DeliveryStatus.SUCCESS.name())
                        .set(NoticeMailRecipient::getSendStatus, DeliveryStatus.FAILED.name())
                        .set(NoticeMailRecipient::getFailureReason, "MAIL_TASK_FAILED"));
        mails.update(
                null,
                leasedDelivery(id, token)
                        .set(NoticeMail::getSendStatus, MailStatus.FAILED.name())
                        .set(NoticeMail::getFailureReason, "MAIL_TASK_FAILED")
                        .set(NoticeMail::getSuccessCount, countDeliveries(id, DeliveryStatus.SUCCESS))
                        .set(NoticeMail::getFailureCount, countDeliveries(id, DeliveryStatus.FAILED))
                        .set(NoticeMail::getLeaseToken, null)
                        .set(NoticeMail::getLeaseUntil, null));
    }

    /** 构建同时匹配邮件ID和租约标识的更新条件。 */
    private LambdaUpdateWrapper<NoticeMail> leasedDelivery(Long id, String token) {
        return Wrappers.<NoticeMail>lambdaUpdate()
                .eq(NoticeMail::getId, id)
                .eq(NoticeMail::getLeaseToken, token);
    }
}
