package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 部门主管候选员工。
 */
@Data
@AllArgsConstructor
@Schema(description = "部门主管候选员工")
public class DeptLeaderCandidateResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "所属部门名称")
    private String deptName;
}
