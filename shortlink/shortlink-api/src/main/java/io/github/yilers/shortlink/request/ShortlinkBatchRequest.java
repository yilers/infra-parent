package io.github.yilers.shortlink.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** 同一路径下的一次批量创建；重试时必须保持完整请求不变。 */
@Data
@Schema(description = "批量创建短链")
public class ShortlinkBatchRequest {
    @NotBlank @Size(max = 128)
    @Schema(description = "配置内唯一的批次标识，重试时保持不变")
    private String batchId;
    @NotNull
    @Schema(description = "短链路径ID")
    private Long namespaceId;
    @Schema(description = "批次默认失效时间")
    private LocalDateTime expireTime;
    @Schema(description = "批次默认永久有效，与失效时间互斥")
    private boolean permanent;
    @Valid @NotEmpty @Size(max = 1000)
    @Schema(description = "按顺序排列的短链明细，重试时不得改变顺序")
    private List<@NotNull @Valid Item> items;

    @Data
    @Schema(description = "批量创建明细")
    public static class Item {
        @Size(max = 128)
        @Schema(description = "批次内可选业务唯一标识，不可重复")
        private String businessId;
        @Size(max = 4096)
        private String targetUrl;
        @Size(max = 512)
        private String businessCode;
        @Pattern(regexp = "[0-9A-Za-z]{6}|[0-9A-Za-z]{8}")
        private String shortCode;
        @Schema(description = "覆盖批次默认失效时间")
        private LocalDateTime expireTime;
        @Schema(description = "覆盖批次默认永久有效设置")
        private Boolean permanent;
    }
}
