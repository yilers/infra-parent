package io.github.yilers.notice.service;

import static io.github.yilers.notice.support.NoticeSupport.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.yilers.notice.entity.NoticeMessageRecipient;
import io.github.yilers.notice.i18n.NoticeMessageCode;
import io.github.yilers.notice.mapper.*;
import io.github.yilers.notice.model.*;
import io.github.yilers.notice.support.NoticeSupport;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/** 个人消息不要求后台管理权限，但必须登录且属于实际接收人。 */
@Service
@RequiredArgsConstructor
public class NoticeInboxServiceImpl implements NoticeInboxService {
    /** 按当前租户和接收用户查询个人消息。 */
    private final NoticeInboxMapper inbox;

    /** 接收记录持久化。 */
    private final NoticeMessageRecipientMapper recipients;

    /** 按消息分类和阅读状态查询当前用户的收件箱。 */
    @Override
    public NoticePage<NoticeInboxItem> page(String type, Boolean unread, int current, int size) {
        NoticeSupport.page(current, size);
        Page<NoticeInboxItem> page = new Page<>(current, size);
        var rows = inbox.page(page, tenant(), user(), type, unread);
        return new NoticePage<>(page.getTotal(), rows);
    }

    /** 通过租户、用户和收件记录ID定位当前用户可读的消息。 */
    @Override
    public NoticeInboxItem detail(Long id) {
        var item = inbox.detail(tenant(), user(), id);
        require(item != null, NoticeMessageCode.NOT_FOUND);
        return item;
    }

    /** 统计当前用户各分类及总未读数。 */
    @Override
    public Map<String, Long> unread() {
        Map<String, Long> values = new LinkedHashMap<>();
        long total = 0;
        for (var row : inbox.unread(tenant(), user())) {
            String type = row.messageType();
            long count = row.unreadCount();
            values.put(type, count);
            total += count;
        }
        values.put("total", total);
        return values;
    }

    /** 标记为已读，保留首次阅读时间。 */
    @Override
    public void read(Long id) {
        detail(id);
        recipients.update(
                null,
                Wrappers.<NoticeMessageRecipient>lambdaUpdate()
                        .eq(NoticeMessageRecipient::getId, id)
                        .eq(NoticeMessageRecipient::getUserId, user())
                        .isNull(NoticeMessageRecipient::getReadTime)
                        .set(NoticeMessageRecipient::getReadTime, LocalDateTime.now())
                        .set(NoticeMessageRecipient::getUpdateTime, LocalDateTime.now()));
    }

    /** 从当前用户的收件箱移除消息，保留接收记录。 */
    @Override
    public void remove(Long id) {
        detail(id);
        recipients.update(
                null,
                Wrappers.<NoticeMessageRecipient>lambdaUpdate()
                        .eq(NoticeMessageRecipient::getId, id)
                        .eq(NoticeMessageRecipient::getUserId, user())
                        .set(NoticeMessageRecipient::getRemovedTime, LocalDateTime.now())
                        .set(NoticeMessageRecipient::getUpdateTime, LocalDateTime.now()));
    }

    /** 将当前用户收件箱中可见的未读消息标为已读。 */
    @Override
    public void readAll() {
        inbox.readAll(tenant(), user(), LocalDateTime.now());
    }
}
