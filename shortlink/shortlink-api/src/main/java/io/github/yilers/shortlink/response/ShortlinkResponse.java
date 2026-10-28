package io.github.yilers.shortlink.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "短链创建或查询结果")
public record ShortlinkResponse(
        @Schema(description = "短链ID") Long id,
        @Schema(description = "幂等请求标识") String requestId,
        @Schema(description = "批次标识，单条创建为空") String batchId,
        @Schema(description = "批次内业务标识") String businessId,
        @Schema(description = "路径段") String namespace,
        @Schema(description = "短码") String code,
        @Schema(description = "完整短链") String shortUrl,
        @Schema(description = "有效期，空表示永久") LocalDateTime expireTime,
        @Schema(description = "启用状态") Integer usable,
        @Schema(description = "乐观锁版本") Integer version) {
}
