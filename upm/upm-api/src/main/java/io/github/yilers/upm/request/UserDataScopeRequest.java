package io.github.yilers.upm.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserDataScopeRequest {

    @NotNull(message = "用户id不能为空")
    private Long userId;

    private List<String> interfacePathList;

}
