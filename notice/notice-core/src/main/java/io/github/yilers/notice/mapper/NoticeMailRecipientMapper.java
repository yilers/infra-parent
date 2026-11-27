package io.github.yilers.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.github.yilers.notice.entity.NoticeMailRecipient;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 接收快照批量写入，只允许使用服务端构造的同租户记录。 */
public interface NoticeMailRecipientMapper extends BaseMapper<NoticeMailRecipient> {
    @Insert(
            """
            <script>INSERT INTO notice_mail_recipient (id, tenant_id, mail_id, user_id, email, recipient_name, params, send_status, attempt_count, retry_limit, create_time, update_time) VALUES <foreach collection="rows" item="r" separator=",">(#{r.id}, #{r.tenantId}, #{r.mailId}, #{r.userId}, #{r.email}, #{r.recipientName}, #{r.params}, #{r.sendStatus}, #{r.attemptCount}, #{r.retryLimit}, #{r.createTime}, #{r.updateTime})</foreach></script>
            """)
    int insertBatch(@Param("rows") List<NoticeMailRecipient> rows);
}
