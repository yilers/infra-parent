package io.github.yilers.notice.service;

import io.github.yilers.notice.entity.NoticeMessage;
import io.github.yilers.notice.entity.NoticeMessageRecipient;
import io.github.yilers.notice.model.NoticePage;
import io.github.yilers.notice.model.NoticeUser;
import io.github.yilers.notice.request.MessageSaveRequest;
import io.github.yilers.notice.request.NoticeVersionRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 业务通过Service提交持久化站内消息任务，外层业务事务回滚时任务一并回滚。 */
public interface MessageService {
    /** 创建并提交发布，重复requestId返回原任务，不重复发送。 */
    Long send(MessageSaveRequest request);

    /** 保存或编辑站内通知草稿。 */
    Long save(MessageSaveRequest request);

    /** 提交已有通知，按计划时间异步发布。 */
    void publish(NoticeVersionRequest request);

    /** 分页查询当前租户的站内通知，批量补充发布后的已读和未读人数。 */
    NoticePage<NoticeMessage> page(String keyword, String messageType, int current, int size);

    /** 按ID查询当前租户的通知详情。 */
    NoticeMessage detail(Long id);

    /** 按版本删除草稿或发布失败的通知。 */
    void delete(NoticeVersionRequest request);

    /** 统计通知的接收、已读和未读人数。 */
    Map<String, Long> counts(Long id);

    /** 按阅读状态分页查询通知的接收记录。 */
    NoticePage<NoticeMessageRecipient> recipients(Long id, Boolean read, int current, int size);

    /** 后台抢占保留到期时间和租约条件，通过同一条更新避免多个节点同时接手。 */
    boolean claimPublication(Long id, String token, LocalDateTime now, LocalDateTime until);

    /** 仅允许持有当前租约的任务延长到期时间。 */
    boolean renewPublicationLease(Long id, String token, LocalDateTime until);

    /** 后台发布需要读取默认列表查询不加载的接收快照。 */
    NoticeMessage findForPublication(Long id);

    /** 由任务在续租的同一事务中调用。补捞时跳过已经生成的收件记录。 */
    void addPublicationRecipients(Long id, String token, List<NoticeUser> batch);

    /** 以当前租约完成发布，记录发布时间并释放租约。 */
    void completePublication(Long id, String token);

    /** 以当前租约标记发布失败，保留接收快照供重试复用。 */
    void failPublication(Long id, String token);
}
