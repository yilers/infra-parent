package io.github.yilers.notice.request;

import io.github.yilers.notice.model.MailReceiver;
import io.github.yilers.notice.model.NoticeTarget;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 业务按租户内稳定编码发送模板邮件，调用方不需要查询内部主键。 */
@Schema(description = "按编码提交模板邮件")
public record MailTemplateSendRequest(
        @Schema(description = "发件配置编码") String configCode,
        @Schema(description = "邮件模板编码") String templateCode,
        @Schema(description = "公共变量参数") Map<String, String> params,
        @Schema(description = "接收邮箱及个人变量") List<MailReceiver> recipients,
        @Schema(description = "可选租户内接收范围") NoticeTarget target,
        @Schema(description = "来源业务类型") String businessType,
        @Schema(description = "业务ID或批次号") String businessId,
        @Schema(description = "可选创建幂等号") String requestId,
        @Schema(description = "计划发送时间，空表示立即执行") LocalDateTime scheduledTime) {}
