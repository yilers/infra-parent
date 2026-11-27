package io.github.yilers.notice.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.yilers.notice.model.NoticeInboxItem;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** 个人收件箱查询，发布成功后才可见；租户与接收用户两层约束均不可省略。 */
public interface NoticeInboxMapper {
    record UnreadCount(String messageType, Long unreadCount) {}

    @Select(
            """
            <script>
            SELECT r.id, m.id AS message_id, m.title, m.message_type, NULL AS content, m.content_type, m.action_url, r.read_time, m.publish_time
            FROM notice_message_recipient r JOIN notice_message m ON r.message_id=m.id AND r.tenant_id=m.tenant_id
            WHERE r.tenant_id=#{tenantId} AND r.user_id=#{userId} AND r.removed_time IS NULL
            AND m.deleted=0 AND m.publish_status='PUBLISHED'
            <if test="type != null and type != ''">AND m.message_type=#{type}</if>
            <if test="unread != null and unread">AND r.read_time IS NULL</if>
            ORDER BY m.publish_time DESC,r.id DESC
            </script>
            """)
    List<NoticeInboxItem> page(
            Page<NoticeInboxItem> page,
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("unread") Boolean unread);

    @Select(
            """
            SELECT r.id, m.id AS message_id, m.title, m.message_type, m.content, m.content_type, m.action_url, r.read_time, m.publish_time
            FROM notice_message_recipient r JOIN notice_message m ON r.message_id=m.id AND r.tenant_id=m.tenant_id
            WHERE r.tenant_id=#{tenantId} AND r.user_id=#{userId} AND r.id=#{id} AND r.removed_time IS NULL
            AND m.deleted=0 AND m.publish_status='PUBLISHED'
            """)
    NoticeInboxItem detail(@Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("id") Long id);

    @Select(
            """
            SELECT m.message_type,COUNT(*) AS unread_count
            FROM notice_message_recipient r JOIN notice_message m ON r.message_id=m.id AND r.tenant_id=m.tenant_id
            WHERE r.tenant_id=#{tenantId} AND r.user_id=#{userId} AND r.read_time IS NULL AND r.removed_time IS NULL
            AND m.deleted=0 AND m.publish_status='PUBLISHED' GROUP BY m.message_type
            """)
    List<UnreadCount> unread(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Update(
            """
            UPDATE notice_message_recipient SET read_time=#{now},update_time=#{now}
            WHERE tenant_id=#{tenantId} AND user_id=#{userId} AND read_time IS NULL AND removed_time IS NULL
            AND message_id IN (SELECT id FROM notice_message WHERE tenant_id=#{tenantId} AND deleted=0 AND publish_status='PUBLISHED')
            """)
    int readAll(
            @Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("now") java.time.LocalDateTime now);
}
