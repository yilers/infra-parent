package com.github.yilers.web.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class BaseOperateRequest {

    @NotNull(message = "id不能为空")
    @Schema(description = "id")
    private Long id;

}
