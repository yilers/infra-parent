package com.github.yilers.upm.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class RolePermissionRequest {

    @NotNull(message = "角色id不能为空")
    private Long roleId;

    private List<Long> permissionIdList;

    @NotBlank(message = "设备类型不能为空")
    private String device;
}
