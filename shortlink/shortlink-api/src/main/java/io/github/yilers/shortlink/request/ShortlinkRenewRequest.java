package io.github.yilers.shortlink.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/** 续期仅改变有效期，不自动启用已停用的短链。 */
@Data
@Schema(description = "短链续期请求")
public class ShortlinkRenewRequest {
    @NotNull @Schema(description = "短链ID") private Long id;
    @NotNull @Schema(description = "当前乐观锁版本") private Integer version;
    @Schema(description = "新的明确到期时间；必须晚于当前时间且不能缩短原期限") private LocalDateTime expireTime;
    @Schema(description = "改为永久有效；与expireTime互斥") private boolean permanent;
}
