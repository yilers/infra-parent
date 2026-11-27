package io.github.yilers.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.github.yilers.notice.entity.NoticeMessageRecipient;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 接收快照批量写入，只允许使用服务端构造的同租户记录。 */
public interface NoticeMessageRecipientMapper extends BaseMapper<NoticeMessageRecipient> {
    record ReadCount(Long messageId, Long readCount) {}

    @org.apache.ibatis.annotations.Select(
            """
            <script>
            SELECT message_id,COUNT(*) AS read_count FROM notice_message_recipient
            WHERE read_time IS NOT NULL AND message_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY message_id
            </script>
            """)
    List<ReadCount> readCounts(@Param("ids") List<Long> ids);

    @Insert(
            """
            <script>INSERT INTO notice_message_recipient (id, tenant_id, message_id, user_id, recipient_name, create_time, update_time) VALUES <foreach collection="rows" item="r" separator=",">(#{r.id}, #{r.tenantId}, #{r.messageId}, #{r.userId}, #{r.recipientName}, #{r.createTime}, #{r.updateTime})</foreach></script>
            """)
    int insertBatch(@Param("rows") List<NoticeMessageRecipient> rows);
}
