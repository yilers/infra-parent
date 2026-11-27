package io.github.yilers.notice.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.github.yilers.notice.model.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 邮件草稿，支持外部邮箱请求。 */
@Schema(description = "邮件草稿，支持外部邮箱请求。")
public record MailSaveRequest(
        @Schema(description = "记录ID，新建为空") Long id,
        @Schema(description = "乐观锁版本，修改必传") Integer version,
        @Schema(description = "当前租户启用的SMTP配置ID") Long configId,
        @Schema(description = "可选邮件模板ID；选择后使用模板快照") Long templateId,
        @Schema(description = "邮件主题，支持已声明的变量") String subject,
        @Schema(description = "正文格式：TEXT、HTML、MARKDOWN") String contentType,
        @Schema(description = "正文或模板源码") String content,
        @Schema(description = "变量定义，仅支持字母数字下划线，不执行表达式") List<TemplateVariable> variables,
        @Schema(description = "公共模板参数，个人参数可覆盖") Map<String, String> commonParams,
        @Schema(description = "外部邮箱或指定接收人；编辑时为空且未传target则保留原接收名单") List<MailReceiver> recipients,
        @Schema(description = "本租户接收范围；与外部邮箱合并后去重") NoticeTarget target,
        @Schema(description = "来源业务类型") String businessType,
        @Schema(description = "业务ID或业务批次ID，可关联多次通知") String businessId,
        @Schema(description = "可选创建幂等号，同号必须为同一请求内容") String requestId,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") @Schema(description = "计划时间，空表示立即执行；使用北京时间")
                LocalDateTime scheduledTime) {}
