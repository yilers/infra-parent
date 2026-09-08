package io.github.yilers.upm.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionBatchRequest {
    @NotNull(message = "角色id不能为空")
    private Long roleId;

    @Valid
    @NotEmpty(message = "请选择要修改的权限范围")
    private List<RolePermissionRequest> scopes;
}
