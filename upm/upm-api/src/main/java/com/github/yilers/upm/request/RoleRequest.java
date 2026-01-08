package com.github.yilers.upm.request;

import com.github.yilers.web.validated.Add;
import com.github.yilers.web.validated.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;


@Data
public class RoleRequest {

    @NotNull(message = "id不能为空", groups = Update.class)
    @Null(message = "新增不能填id", groups = Add.class)
    @Schema(description = "主键id")
    private Long id;

    @NotBlank(message = "名称不能为空")
    @Schema(description = "角色名称")
    private String roleName;

    @NotBlank(message = "编码不能为空")
    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "数据权限类型 1-全部 2-本部门及以下 3-本部门 4-自定义部门")
    @NotNull(message = "数据权限类型")
    private Integer dataScope;

    @Schema(description = "角色描述")
    private String roleDesc;

    @Min(value = 0, message = "角色状态值只能是1或0")
    @Max(value = 1, message = "角色状态值只能是1或0")
    @Schema(description = "是否可用 1-启用 0-禁用")
    private Integer usable;

    @NotNull(message = "版本号不能为空")
    private Integer version;

    @Schema(description = "扩展字段")
    private String expand;

    @Schema(description = "当数据权限为自定义时角色部门id列表")
    private List<Long> roleDeptIdList;

}
