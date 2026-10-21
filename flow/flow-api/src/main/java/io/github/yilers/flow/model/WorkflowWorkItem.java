package io.github.yilers.flow.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 审批中心列表摘要。只返回展示所需字段，不返回流程变量、监听器和业务正文。
 */
@Data
@Schema(description = "审批中心列表项")
public class WorkflowWorkItem {

    @Schema(description = "列表记录ID：待办为任务ID，已办为历史记录ID，发起列表为实例ID，抄送为接收记录ID")
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
    @Schema(description = "流程节点编码，用于可信业务任务识别，不使用节点名称判断业务行为")
    private String nodeCode;
    @Schema(description = "业务表单提交任务关联；非空时须走业务表单办理，不能使用通用通过或退回")
    private WorkflowBusinessReference submission;
    @Schema(description = "当前实例状态，采用Warm-Flow状态编码")
    private String flowStatus;
    @Schema(description = "发起人用户ID")
    private String initiatorId;
    @Schema(description = "发起人当前展示姓名；未查到用户时为空")
    private String initiatorName;
    @Schema(description = "实际办理人用户ID；未办理时为空")
    private String approverId;
    @Schema(description = "实际办理人当前展示姓名；未查到用户时为空")
    private String approverName;
    @Schema(description = "办理动作：PASS通过、REJECT退回、NONE其他操作")
    private String skipType;
    @Schema(description = "审批意见")
    private String message;
    @Schema(description = "记录创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @Schema(description = "记录更新时间；已办列表中为办理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    @Schema(description = "抄送首次阅读时间；非抄送列表为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date readTime;
    @Schema(description = "是否为本人当前可办理任务，不替代提交时的再次校验")
    private boolean actionable;
}
