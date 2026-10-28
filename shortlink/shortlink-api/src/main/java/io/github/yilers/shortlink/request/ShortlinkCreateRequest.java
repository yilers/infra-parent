package io.github.yilers.shortlink.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 单条创建请求；不允许客户端传入tenantId、创建人或凭证ID。 */
@Data
@Schema(description = "创建短链")
public class ShortlinkCreateRequest {
    @NotNull
    @Schema(description = "目标namespace ID")
    private Long namespaceId;
    @NotBlank @Size(max = 128)
    @Schema(description = "配置内唯一幂等标识；重试保持不变，新建使用新值")
    private String requestId;
    @Size(max = 4096)
    @Schema(description = "URL模式必填的完整HTTP/HTTPS目标地址")
    private String targetUrl;
    @Size(max = 512)
    @Schema(description = "TEMPLATE模式必填的业务编码")
    private String businessCode;
    @Pattern(regexp = "[0-9A-Za-z]{6}|[0-9A-Za-z]{8}")
    @Schema(description = "CUSTOM模式必填，AUTO模式不允许传入")
    private String shortCode;
    @Schema(description = "明确的失效时间；与permanent=true互斥；均未指定时使用配置默认值")
    private LocalDateTime expireTime;
    @Schema(description = "明确申请永久有效，默认false")
    private boolean permanent;
}
