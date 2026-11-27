package io.github.yilers.notice.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/** 唯一跨租户入口：仅供受信任的后台扫描，不能暴露HTTP。 扫描只获取任务定位；抢占显式绑定tenant_id，业务处理随后恢复该任务的租户上下文。 */
public interface NoticeTaskMapper {
    record Task(Long id, Long tenantId, String kind) {}

    @InterceptorIgnore(tenantLine = "true")
    @Select(
            """
            SELECT id, tenant_id, 'MESSAGE' AS kind FROM notice_message
                    WHERE deleted=0 AND publish_status IN ('SCHEDULED','PUBLISHING')
                    AND (scheduled_time IS NULL OR scheduled_time <= #{now})
                    AND (lease_until IS NULL OR lease_until < #{now})
            ORDER BY id LIMIT 20
            """)
    List<Task> dueMessages(@Param("now") LocalDateTime now);

    @InterceptorIgnore(tenantLine = "true")
    @Select(
            """
                SELECT id, tenant_id, 'MAIL' AS kind FROM notice_mail
                WHERE deleted=0 AND send_status IN ('SCHEDULED','SENDING')
                AND (scheduled_time IS NULL OR scheduled_time <= #{now})
                AND (lease_until IS NULL OR lease_until < #{now})
                AND (
              EXISTS (SELECT 1 FROM notice_mail_recipient r WHERE r.mail_id=notice_mail.id AND r.tenant_id=notice_mail.tenant_id
                AND (r.send_status IN ('PENDING','SENDING') OR
                  (r.send_status='RETRY_WAIT' AND (r.next_retry_time IS NULL OR r.next_retry_time <= #{now}))))
              OR NOT EXISTS (SELECT 1 FROM notice_mail_recipient r WHERE r.mail_id=notice_mail.id AND r.tenant_id=notice_mail.tenant_id
                AND r.send_status IN ('PENDING','SENDING','RETRY_WAIT'))
            )
            ORDER BY id LIMIT 20
            """)
    List<Task> dueMails(@Param("now") LocalDateTime now);
}
