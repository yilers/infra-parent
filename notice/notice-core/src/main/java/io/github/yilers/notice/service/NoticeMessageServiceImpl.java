package io.github.yilers.notice.service;

import static io.github.yilers.notice.support.NoticeSupport.*;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 管理当前租户的站内通知，处理发布快照、任务租约与接收记录。 */
@Service
@RequiredArgsConstructor
public class NoticeMessageServiceImpl implements MessageService {
    /** 站内通知主记录持久化。 */
    private final NoticeMessageMapper messages;

    /** 接收记录持久化。 */
    private final NoticeMessageRecipientMapper recipients;

    /** 通知模块的 JSON 序列化与参数校验工具。 */
    private final NoticeSupport support;

    /** 从租户组织目录解析接收范围。 */
    private final NoticeAudienceService audience;

    /** 发布提交事件，由事务提交后的监听器触发后台处理。 */
    private final ApplicationEventPublisher events;

    /** 分页查询当前租户的站内通知，批量补充发布后的已读和未读人数。 */
    @Override
    public NoticePage<NoticeMessage> page(String keyword, String messageType, int current, int size) {
        tenant();
        NoticeSupport.page(current, size);
        Page<NoticeMessage> result =
                messages.selectPage(
                        new Page<>(current, size),
                        Wrappers.<NoticeMessage>lambdaQuery()
                                .like(keyword != null && !keyword.isBlank(), NoticeMessage::getTitle, keyword)
                                .eq(
                                        messageType != null && !messageType.isBlank(),
                                        NoticeMessage::getMessageType,
                                        messageType)
                                .orderByDesc(NoticeMessage::getCreateTime)
                                .orderByDesc(NoticeMessage::getId));
        if (!result.getRecords().isEmpty()) {
            Map<Long, Long> read = new HashMap<>();
            recipients
                    .readCounts(result.getRecords().stream().map(NoticeMessage::getId).toList())
                    .forEach(row -> read.put(row.messageId(), row.readCount()));
            result.getRecords()
                    .forEach(row -> {
                        long count = read.getOrDefault(row.getId(), 0L);
                        row.setReadCount(count);
                        row.setUnreadCount(Math.max(0, row.getRecipientCount() - count));
                    });
        }
        return new NoticePage<>(result.getTotal(), result.getRecords());
    }

    /** 按ID查询当前租户的通知详情。 */
    @Override
    public NoticeMessage detail(Long id) {
        tenant();
        NoticeMessage value = messages.selectById(id);
        require(value != null, NoticeMessageCode.NOT_FOUND);
        return value;
    }

    /** 保存并提交站内通知，已提交的幂等请求复用原任务。 */
    @Override
    @Transactional
    public Long send(MessageSaveRequest request) {
        Long id = save(request);
        NoticeMessage entity = detail(id);
        if (PublishStatus.DRAFT.name().equals(entity.getPublishStatus()))
            publish(new NoticeVersionRequest(id, entity.getVersion()));
        return id;
    }

    /** 保存通知草稿，编辑时校验版本与当前状态。 */
    @Override
    @Transactional
    public Long save(MessageSaveRequest request) {
        tenant();
        String requestHash = support.fingerprint(request);
        if (request.id() == null && request.requestId() != null && !request.requestId().isBlank()) {
            text(request.requestId(), 128, "requestId");
            var existing =
                    messages.selectOne(
                            Wrappers.<NoticeMessage>lambdaQuery().eq(NoticeMessage::getRequestId, request.requestId()));
            if (existing != null) {
                require(requestHash.equals(existing.getRequestHash()), NoticeMessageCode.CONFLICT);
                return existing.getId();
            }
        }
        NoticeMessage entity;
        if (request.id() == null) {
            entity = new NoticeMessage();
            initialize(entity);
            entity.setPublishStatus(PublishStatus.DRAFT.name());
            entity.setRecipientCount(0L);
        } else {
            entity = detail(request.id());
            version(entity, request.version());
            require(PublishStatus.DRAFT.name().equals(entity.getPublishStatus()), NoticeMessageCode.STATE);
        }
        if (request.id() != null)
            require(
                    java.util.Objects.equals(
                            entity.getRequestId(),
                            request.requestId() == null || request.requestId().isBlank() ? null : request.requestId()),
                    NoticeMessageCode.INVALID,
                    "requestId immutable");
        entity.setTitle(text(request.title(), 200, "title"));
        entity.setContent(text(request.content(), 600000, "content"));
        entity.setContentType(type(ContentType.class, request.contentType()).name());
        entity.setMessageType(type(MessageType.class, request.messageType()).name());
        require(request.target() != null, NoticeMessageCode.TARGET_EMPTY);
        entity.setTargetConfig(support.json(request.target()));
        String url = request.actionUrl();
        require(
                url == null
                        || url.isBlank()
                        || (url.startsWith("/")
                                && !url.startsWith("//")
                                && !url.contains("\\")
                                && !url.contains("\r")
                                && !url.contains("\n")
                                && url.length() <= 1000),
                NoticeMessageCode.INVALID,
                "actionUrl");
        entity.setActionUrl(url);
        entity.setBusinessType(optional(request.businessType(), 100, "businessType"));
        entity.setBusinessId(optional(request.businessId(), 128, "businessId"));
        entity.setRequestId(
                request.requestId() == null || request.requestId().isBlank()
                        ? null
                        : text(request.requestId(), 128, "requestId"));
        entity.setRequestHash(requestHash);
        entity.setScheduledTime(request.scheduledTime());
        entity.setUpdateTime(LocalDateTime.now());
        require(
                (request.id() == null ? messages.insert(entity) : messages.updateById(entity)) == 1,
                NoticeMessageCode.CONFLICT);
        return entity.getId();
    }

    /** 冻结接收范围并提交发布。已到期的通知在事务提交后触发。 */
    @Override
    @Transactional
    public void publish(NoticeVersionRequest request) {
        tenant();
        NoticeMessage entity =
                messages.selectOne(
                        Wrappers.<NoticeMessage>lambdaQuery()
                                .select(NoticeMessage.class, f -> true)
                                .eq(NoticeMessage::getId, request.id()));
        version(entity, request.version());
        require(
                PublishStatus.DRAFT.name().equals(entity.getPublishStatus())
                        || PublishStatus.FAILED.name().equals(entity.getPublishStatus()),
                NoticeMessageCode.STATE);
        // 失败重试使用原快照；新发布在当前事务里固定用户ID和姓名，后续不重新解析组织关系。
        if (entity.getRecipientSnapshot() == null)
            entity.setRecipientSnapshot(
                    support.json(audience.resolve(support.read(entity.getTargetConfig(), NoticeTarget.class))));
        entity.setPublishStatus(PublishStatus.SCHEDULED.name());
        entity.setFailureReason(null);
        entity.setLeaseToken(null);
        entity.setLeaseUntil(null);
        entity.setUpdateTime(LocalDateTime.now());
        require(messages.updateById(entity) == 1, NoticeMessageCode.CONFLICT);
        // 到期任务在事务提交后尝试执行，未来任务由调度器到点扫描。
        if (entity.getScheduledTime() == null || !entity.getScheduledTime().isAfter(LocalDateTime.now()))
            events.publishEvent(new NoticeTaskMapper.Task(entity.getId(), tenant(), TaskKind.MESSAGE.name()));
    }

    /** 按版本删除草稿或发布失败的通知。 */
    @Override
    @Transactional
    public void delete(NoticeVersionRequest request) {
        NoticeMessage entity = detail(request.id());
        version(entity, request.version());
        require(
                PublishStatus.DRAFT.name().equals(entity.getPublishStatus())
                        || PublishStatus.FAILED.name().equals(entity.getPublishStatus()),
                NoticeMessageCode.STATE);
        require(
                messages.update(
                                null,
                                Wrappers.<NoticeMessage>lambdaUpdate()
                                        .eq(NoticeMessage::getId, request.id())
                                        .eq(NoticeMessage::getVersion, request.version())
                                        .set(NoticeMessage::getDeleted, 1))
                        == 1,
                NoticeMessageCode.CONFLICT);
    }

    /** 统计通知的接收、已读和未读人数。 */
    @Override
    public Map<String, Long> counts(Long id) {
        NoticeMessage entity = detail(id);
        long total =
                recipients.selectCount(
                        Wrappers.<NoticeMessageRecipient>lambdaQuery().eq(NoticeMessageRecipient::getMessageId, id));
        long read =
                recipients.selectCount(
                        Wrappers.<NoticeMessageRecipient>lambdaQuery()
                                .eq(NoticeMessageRecipient::getMessageId, id)
                                .isNotNull(NoticeMessageRecipient::getReadTime));
        return Map.of("total", total, "read", read, "unread", total - read);
    }

    /** 按阅读状态分页查询通知的接收记录。 */
    @Override
    public NoticePage<NoticeMessageRecipient> recipients(Long id, Boolean read, int current, int size) {
        detail(id);
        NoticeSupport.page(current, size);
        var query = Wrappers.<NoticeMessageRecipient>lambdaQuery().eq(NoticeMessageRecipient::getMessageId, id);
        if (read != null) {
            if (read) query.isNotNull(NoticeMessageRecipient::getReadTime);
            else query.isNull(NoticeMessageRecipient::getReadTime);
        }
        var result = recipients.selectPage(new Page<>(current, size), query.orderByAsc(NoticeMessageRecipient::getId));
        return new NoticePage<>(result.getTotal(), result.getRecords());
    }

    /** 后台抢占保留到期时间和租约条件，通过同一条更新避免多个节点同时接手。 */
    @Override
    public boolean claimPublication(Long id, String token, LocalDateTime now, LocalDateTime until) {
        return messages.update(
                        null,
                        Wrappers.<NoticeMessage>lambdaUpdate()
                                .eq(NoticeMessage::getId, id)
                                .in(
                                        NoticeMessage::getPublishStatus,
                                        PublishStatus.SCHEDULED.name(),
                                        PublishStatus.PUBLISHING.name())
                                .and(q ->
                                        q.isNull(NoticeMessage::getScheduledTime)
                                                .or()
                                                .le(NoticeMessage::getScheduledTime, now))
                                .and(q ->
                                        q.isNull(NoticeMessage::getLeaseUntil)
                                                .or()
                                                .lt(NoticeMessage::getLeaseUntil, now))
                                .set(NoticeMessage::getLeaseToken, token)
                                .set(NoticeMessage::getLeaseUntil, until)
                                .set(NoticeMessage::getPublishStatus, PublishStatus.PUBLISHING.name()))
                == 1;
    }

    /** 仅允许持有当前租约的任务延长到期时间。 */
    @Override
    public boolean renewPublicationLease(Long id, String token, LocalDateTime until) {
        return messages.update(null, leasedPublication(id, token).set(NoticeMessage::getLeaseUntil, until)) == 1;
    }

    /** 后台发布需要读取默认列表查询不加载的接收快照。 */
    @Override
    public NoticeMessage findForPublication(Long id) {
        return messages.selectOne(
                Wrappers.<NoticeMessage>lambdaQuery()
                        .select(NoticeMessage.class, field -> true)
                        .eq(NoticeMessage::getId, id));
    }

    /** 由任务在续租的同一事务中调用。补捞时跳过已经生成的收件记录。 */
    @Override
    public void addPublicationRecipients(Long id, String token, List<NoticeUser> batch) {
        Set<Long> existing =
                new HashSet<>(
                        recipients
                                .selectList(
                                        Wrappers.<NoticeMessageRecipient>lambdaQuery()
                                                .eq(NoticeMessageRecipient::getMessageId, id)
                                                .in(
                                                        NoticeMessageRecipient::getUserId,
                                                        batch.stream().map(NoticeUser::id).toList()))
                                .stream()
                                .map(NoticeMessageRecipient::getUserId)
                                .toList());
        List<NoticeMessageRecipient> rows = new ArrayList<>();
        for (NoticeUser user : batch)
            if (!existing.contains(user.id())) {
                NoticeMessageRecipient row = new NoticeMessageRecipient();
                row.setId(IdWorker.getId());
                row.setTenantId(tenant());
                row.setMessageId(id);
                row.setUserId(user.id());
                row.setRecipientName(user.name());
                row.setCreateTime(LocalDateTime.now());
                row.setUpdateTime(row.getCreateTime());
                rows.add(row);
            }
        if (!rows.isEmpty()) recipients.insertBatch(rows);
        messages.update(
                null,
                leasedPublication(id, token)
                        .setSql("recipient_count = recipient_count + " + rows.size()));
    }

    /** 以当前租约完成发布，记录发布时间并释放租约。 */
    @Override
    public void completePublication(Long id, String token) {
        messages.update(
                null,
                leasedPublication(id, token)
                        .set(NoticeMessage::getPublishStatus, PublishStatus.PUBLISHED.name())
                        .set(NoticeMessage::getPublishTime, LocalDateTime.now())
                        .set(NoticeMessage::getLeaseToken, null)
                        .set(NoticeMessage::getLeaseUntil, null)
                        .set(NoticeMessage::getUpdateTime, LocalDateTime.now()));
    }

    /** 以当前租约标记发布失败，保留接收快照供重试复用。 */
    @Override
    public void failPublication(Long id, String token) {
        messages.update(
                null,
                leasedPublication(id, token)
                        .set(NoticeMessage::getPublishStatus, PublishStatus.FAILED.name())
                        .set(NoticeMessage::getFailureReason, "PUBLISH_FAILED")
                        .set(NoticeMessage::getLeaseToken, null)
                        .set(NoticeMessage::getLeaseUntil, null));
    }

    /** 构建同时匹配通知ID和租约标识的更新条件。 */
    private LambdaUpdateWrapper<NoticeMessage> leasedPublication(Long id, String token) {
        return Wrappers.<NoticeMessage>lambdaUpdate()
                .eq(NoticeMessage::getId, id)
                .eq(NoticeMessage::getLeaseToken, token);
    }
}
