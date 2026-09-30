package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "批量新增用户的单行结果")
public class UserBatchRowResponse {

    @Schema(description = "前端行标识")
    private String rowKey;

    @Schema(description = "是否新增成功")
    private boolean success;

    @Schema(description = "新增成功后的用户ID")
    private Long userId;

    @Schema(description = "校验失败原因")
    private List<String> messages = new ArrayList<>();
}
