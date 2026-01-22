package io.github.yilers.upm.request;


import io.github.yilers.web.validated.Add;
import io.github.yilers.web.validated.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;



@Data
public class DeptRequest {

    @NotNull(message = "id不能为空", groups = Update.class)
    @Schema(description = "机构id")
    private Long id;

    @NotNull(message = "父级id不能为空", groups = Add.class)
    @Schema(description = "父级机构id", example = "0")
    private Long parentId;

    @NotBlank(message = "机构名称不能为空")
    @Schema(description = "机构名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deptName;

    @NotBlank(message = "机构编码不能为空")
    @Schema(description = "机构编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deptCode;

    @NotNull(message = "状态不能为空", groups = Update.class)
    @Schema(description = "状态", example = "1")
    private Integer usable;

    @Schema(description = "机构描述")
    private String deptDesc;

    @NotNull(message = "层级不能为空", groups = Add.class)
    @Schema(description = "层级/深度")
    private Integer parentDeptDeep;

    @NotNull(message = "版本号不能为空")
    private Integer version;

    @Schema(description = "排序")
    private Integer sortNumber;

}
