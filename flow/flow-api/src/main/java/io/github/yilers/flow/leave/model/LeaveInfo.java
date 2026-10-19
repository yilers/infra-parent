package io.github.yilers.flow.leave.model;

import io.github.yilers.flow.leave.entity.LeaveApplication;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 单据展示数据；审批状态实时关联实例，避免业务表与流程表各存一份状态而失步。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "请假申请详情")
public class LeaveInfo extends LeaveApplication {

    @Schema(description = "DRAFT表示草稿；已提交时为Warm-Flow实例状态编码")
    private String flowStatus;
    @Schema(description = "当前审批节点")
    private String nodeName;
}
