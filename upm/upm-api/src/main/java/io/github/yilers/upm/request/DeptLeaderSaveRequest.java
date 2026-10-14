package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 设置部门当前的主管与副主管。空用户列表表示撤销相应任职。
 */
@Data
@Schema(description = "设置部门主管请求")
public class DeptLeaderSaveRequest {

    @NotNull(message = "部门ID不能为空")
    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long deptId;

    @Schema(description = "主管用户ID；为空表示暂不设置主管")
    private Long primaryUserId;

    @Schema(description = "副主管用户ID列表；为空表示没有副主管")
    private List<Long> deputyUserIds;
}
