package io.github.yilers.notice.service;

import io.github.yilers.notice.entity.NoticeMail;
import io.github.yilers.notice.entity.NoticeMailRecipient;
import io.github.yilers.notice.model.MailPreview;
import io.github.yilers.notice.model.NoticePage;
import io.github.yilers.notice.model.NoticeTypes.DeliveryStatus;
import io.github.yilers.notice.model.NoticeTypes.MailStatus;
import io.github.yilers.notice.request.MailSaveRequest;
import io.github.yilers.notice.request.MailTemplateSendRequest;
import io.github.yilers.notice.request.NoticeVersionRequest;

import java.time.LocalDateTime;
import java.util.List;

/** 邮件发送契约，保存正文和参数快照，实际SMTP调用由后台任务执行。 */
public interface MailService {
    /** 按配置和模板编码提交发送，不要求业务查询模板ID。 */
    Long sendTemplate(MailTemplateSendRequest request);

    /** 创建并提交发送，重复请求号复用原任务。 */
    Long submit(MailSaveRequest request);

    /** 保存或编辑邮件草稿。 */
    Long save(MailSaveRequest request);

    /** 提交已有邮件草稿，按计划时间异步投递。 */
    void send(NoticeVersionRequest request);

    /** 分页查询当前租户的邮件任务。 */
    NoticePage<NoticeMail> page(String keyword, int current, int size);

    /** 按ID查询当前租户的邮件任务。 */
    NoticeMail detail(Long id);

    /** 分页查询邮件收件人及其投递结果。 */
    NoticePage<NoticeMailRecipient> recipients(Long id, int current, int size);

    /** 预览指定邮件任务，优先使用首个收件人的变量。 */
    MailPreview preview(Long id);

    /** 使用任务快照及收件人变量生成邮件内容。 */
    MailPreview preview(NoticeMail mail, NoticeMailRecipient receiver);

    /** 恢复失败的收件记录并提交重试，保留累计尝试次数。 */
    void retry(NoticeVersionRequest request);

    /** 按版本删除邮件草稿。 */
    void delete(NoticeVersionRequest request);

    /** 后台抢占只处理已到期且租约可用的邮件。 */
    boolean claimDelivery(Long id, String token, LocalDateTime now, LocalDateTime until);

    /** 仅允许持有当前租约的任务延长到期时间。 */
    boolean renewDeliveryLease(Long id, String token, LocalDateTime until);

    /** 记录邮件任务首次开始投递的时间。 */
    void markDeliveryStarted(Long id, String token, LocalDateTime now);

    /** 按收件记录ID顺序读取本轮可投递的记录，包含到期重试及中断恢复。 */
    List<NoticeMailRecipient> findDueDeliveries(Long id, LocalDateTime now, int limit);

    /** 在任务续租的事务内保存单个收件人的尝试状态和结果。 */
    void updateDeliveryRecipient(NoticeMailRecipient recipient);

    /** 按状态统计邮件的收件记录数。 */
    long countDeliveries(Long mailId, DeliveryStatus state);

    /** 写入邮件汇总进度并释放租约，全部完成时记录完成时间。 */
    void recordDeliveryProgress(
            Long id, String token, long success, long failed, MailStatus state, boolean complete);

    /** 由任务在续租的同一事务中调用，保留已成功的收件记录。 */
    void failDelivery(Long id, String token);
}
