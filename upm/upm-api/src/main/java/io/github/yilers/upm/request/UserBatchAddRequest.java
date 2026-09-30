package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量新增用户请求")
public class UserBatchAddRequest {

    @Valid
    @NotEmpty(message = "用户数据不能为空")
    @Size(max = 500, message = "单次最多新增500个用户")
    @Schema(description = "待新增用户，单次最多500条")
    private List<UserBatchRowRequest> users;
}
