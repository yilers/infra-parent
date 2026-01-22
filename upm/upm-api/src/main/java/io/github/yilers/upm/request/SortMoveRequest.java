package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资源拖动DTO
 * @author hui.zhang
 * @since 2021/1/28 5:14 下午
 **/

@Data
public class SortMoveRequest {

    @NotNull(message = "id不能为空")
    @Schema(description = "第一个资源id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fId;

    @NotNull(message = "id不能为空")
    @Schema(description = "第二个资源id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sId;
}
