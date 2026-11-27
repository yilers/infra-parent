package io.github.yilers.notice.service;

import io.github.yilers.notice.model.NoticeInboxItem;
import io.github.yilers.notice.model.NoticePage;

import java.util.Map;

/** 当前登录用户的个人收件箱契约。 */
public interface NoticeInboxService {
    /** 按消息分类和阅读状态查询当前用户的收件箱。 */
    NoticePage<NoticeInboxItem> page(String type, Boolean unread, int current, int size);

    /** 通过租户、用户和收件记录ID定位当前用户可读的消息。 */
    NoticeInboxItem detail(Long id);

    /** 统计当前用户各分类及总未读数。 */
    Map<String, Long> unread();

    /** 标记为已读，保留首次阅读时间。 */
    void read(Long id);

    /** 从当前用户的收件箱移除消息，保留接收记录。 */
    void remove(Long id);

    /** 将当前用户收件箱中可见的未读消息标为已读。 */
    void readAll();
}
