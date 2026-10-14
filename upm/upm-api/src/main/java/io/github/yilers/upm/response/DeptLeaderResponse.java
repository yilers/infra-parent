package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 部门主管展示信息，不包含用户密码等敏感字段。
 */
@Data
@Schema(description = "部门主管信息")
public class DeptLeaderResponse {

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "主管用户ID")
    private Long userId;

    @Schema(description = "主管姓名")
    private String userName;

    @Schema(description = "主管用户所属部门名称")
    private String userDeptName;

    @Schema(description = "主管类型：1-主管，2-副主管")
    private Integer leaderType;

    @Schema(description = "任职开始日期")
    private LocalDate startDate;
}
