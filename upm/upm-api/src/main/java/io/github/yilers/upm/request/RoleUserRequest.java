package io.github.yilers.upm.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleUserRequest {

    @NotNull(message = "角色id不能为空")
    private Long roleId;

    private List<Long> userIdList;
}
