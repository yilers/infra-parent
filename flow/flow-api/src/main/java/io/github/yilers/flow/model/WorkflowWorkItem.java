package io.github.yilers.flow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 审批中心列表摘要。只返回展示所需字段，不返回流程变量、监听器和业务正文。
 */
@Data
@Schema(description = "审批中心列表项")
public class WorkflowWorkItem {

    @Schema(description = "列表记录ID：待办为任务ID，已办为历史记录ID，发起列表为实例ID")
    private Long id;
    @Schema(description = "流程实例ID")
    private Long instanceId;
    @Schema(description = "任务ID；我发起的列表中为空")
    private Long taskId;
    @Schema(description = "业务单据ID，不代表已授予对应业务正文的访问权限")
    private String businessId;
    @Schema(description = "流程编码")
    private String flowCode;
    @Schema(description = "流程名称")
    private String flowName;
    @Schema(description = "流程版本")
    private String version;
    @Schema(description = "节点名称")
    private String nodeName;
    @Schema(description = "当前实例状态，采用Warm-Flow状态编码")
    private String flowStatus;
    @Schema(description = "发起人用户ID")
    private String initiatorId;
    @Schema(description = "实际办理人用户ID；未办理时为空")
    private String approverId;
    @Schema(description = "办理动作：PASS通过、REJECT退回、NONE其他操作")
    private String skipType;
    @Schema(description = "审批意见")
    private String message;
    @Schema(description = "记录创建时间")
    private Date createTime;
    @Schema(description = "记录更新时间；已办列表中为办理时间")
    private Date updateTime;
    @Schema(description = "是否为本人当前可办理任务，不替代提交时的再次校验")
    private boolean actionable;
}
