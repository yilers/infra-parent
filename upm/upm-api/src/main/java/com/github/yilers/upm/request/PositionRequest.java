package com.github.yilers.upm.request;


import com.github.yilers.web.validated.Add;
import com.github.yilers.web.validated.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class PositionRequest {

    @NotNull(message = "id不能为空", groups = Update.class)
    @Null(message = "新增不能填id", groups = Add.class)
    @Schema(description = "主键id")
    private Long id;

    @NotBlank(message = "名称不能为空")
    @Schema(description = "名称")
    private String positionName;

    @NotBlank(message = "编码不能为空")
    @Schema(description = "编码")
    private String positionCode;

    @Schema(description = "描述")
    private String positionDesc;

    @Min(value = 0, message = "状态值只能是1或0")
    @Max(value = 1, message = "状态值只能是1或0")
    @Schema(description = "是否可用 1-启用 0-禁用")
    private Integer usable;

    @Schema(description = "排序")
    private Integer sortNumber;

    @NotNull(message = "版本号不能为空")
    private Integer version;
}
