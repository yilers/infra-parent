package io.github.yilers.upm.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class UserDataScopeRequest {

    @NotNull(message = "用户id不能为空")
    private Long userId;

    @Valid
    @NotNull(message = "数据权限配置不能为空")
    private List<@NotNull UserDataScopeItemRequest> scopes;

}
