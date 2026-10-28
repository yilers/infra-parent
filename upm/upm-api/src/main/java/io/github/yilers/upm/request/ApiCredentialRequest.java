package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/** 调用凭证管理请求。密钥和租户不可由客户端指定。 */
@Data
@Schema(description = "创建或修改系统调用凭证")
public class ApiCredentialRequest {
    @Schema(description = "修改时必填的凭证ID")
    private Long id;
    @Schema(description = "修改时必填的乐观锁版本")
    private Integer version;
    @NotBlank @Size(max = 100)
    @Schema(description = "调用方名称")
    private String name;
    @Schema(description = "失效时间；空表示长期有效")
    private LocalDateTime expireTime;
    @NotNull @Min(1) @Max(60000)
    @Schema(description = "每分钟调用上限")
    private Integer rateLimit = 600;
    @NotNull @Min(0) @Max(1)
    @Schema(description = "1启用，0停用")
    private Integer usable = 1;
    @NotNull @Size(max = 200)
    @Schema(description = "授予的接口能力编码，只能选择已注册能力")
    private Set<@NotBlank @Size(max = 128) String> permissions = Set.of();
}
