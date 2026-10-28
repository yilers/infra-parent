package io.github.yilers.shortlink.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "短链配置管理请求")
public class ShortlinkConfigRequest {
    @Schema(description = "修改时必填ID") private Long id;
    @Schema(description = "修改时必填版本") private Integer version;
    @NotBlank @Size(max = 100) @Schema(description = "配置名称") private String name;
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9_-]{0,63}") @Schema(description = "租户内唯一编码，创建后不可修改") private String code;
    @NotBlank @Pattern(regexp = "URL|TEMPLATE") @Schema(description = "目标类型，创建后不可修改") private String targetType;
    @Size(max = 2048) @Schema(description = "TEMPLATE模式的URL模板，包含{code}") private String urlTemplate;
    @Min(1) @Max(315360000) @Schema(description = "默认有效秒数，空为永久，最多十年") private Long defaultValidSeconds;
    @NotNull @Min(0) @Max(1) @Schema(description = "启用状态") private Integer usable = 1;
}
