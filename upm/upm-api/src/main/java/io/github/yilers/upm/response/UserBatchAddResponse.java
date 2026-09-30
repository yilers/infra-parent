package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量新增用户结果")
public class UserBatchAddResponse {

    @Schema(description = "提交总数")
    private int totalCount;

    @Schema(description = "新增成功数")
    private int successCount;

    @Schema(description = "新增失败数")
    private int failureCount;

    @Schema(description = "逐行处理结果，顺序与请求保持一致")
    private List<UserBatchRowResponse> rows;
}
