package io.github.yilers.shortlink.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "短链路径管理请求，标识和生成规则创建后固定")
public class ShortlinkNamespaceRequest {
    @Schema(description = "修改时必填ID") private Long id;
    @Schema(description = "修改时必填版本") private Integer version;
    @NotNull @Schema(description = "所属配置ID") private Long configId;
    @NotNull @Schema(description = "所属域名ID") private Long domainId;
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9]{0,15}") @Schema(description = "路径段，如a") private String pathCode;
    @NotBlank @Pattern(regexp = "AUTO|CUSTOM") @Schema(description = "生成模式") private String generateType;
    @NotNull @Schema(description = "短码长度，6或8") private Integer codeLength = 6;
    @NotNull @Min(0) @Max(1) @Schema(description = "启用状态") private Integer usable = 1;
}
