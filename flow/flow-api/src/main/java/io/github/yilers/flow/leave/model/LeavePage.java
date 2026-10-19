package io.github.yilers.flow.leave.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 本人请假申请分页结果。 */
@Schema(description = "本人请假申请分页")
public record LeavePage(@Schema(description = "总条数") long total,
                        @Schema(description = "当前页记录") List<LeaveInfo> records) {
}
