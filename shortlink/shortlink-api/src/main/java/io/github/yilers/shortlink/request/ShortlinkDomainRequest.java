package io.github.yilers.shortlink.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "短链域名管理请求")
public class ShortlinkDomainRequest {
    @Schema(description = "修改时必填ID") private Long id;
    @Schema(description = "修改时必填版本") private Integer version;
    @NotBlank @Size(max = 253) @Schema(description = "域名，不含协议端口路径；创建后不可修改") private String host;
    @NotBlank @Pattern(regexp = "https|http") @Schema(description = "协议，生产使用https") private String scheme = "https";
    @Min(1) @Max(65535) @Schema(description = "对外端口，空表示默认端口；创建后不可修改") private Integer publicPort;
    @NotNull @Min(0) @Max(1) @Schema(description = "启用状态；启用前确认DNS与证书") private Integer usable = 1;
}
