package io.github.yilers.flow.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假示例业务单据，与通用引擎实体分离。
 * Flow表租户为字符串；不继承UPM的Long租户基础实体，避免租户字段类型混用。
 */
@Data
@TableName("flow_ext_leave")
@Schema(description = "请假申请业务记录")
public class LeaveApplication {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "申请单ID")
    private Long id;
    @Schema(description = "租户ID，由服务端设置")
    private String tenantId;
    @Schema(description = "申请人用户ID")
    private String applicantId;
    @Schema(description = "申请人姓名快照")
    private String applicantName;
    @Schema(description = "请假类型：PERSONAL事假、SICK病假、ANNUAL年假、COMP_TIME调休、OTHER其他")
    private String leaveType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "请假开始时间")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "请假结束时间，须晚于开始时间")
    private LocalDateTime endTime;
    @Schema(description = "实际请假小时数，按0.5小时递增；不自动计算节假日和排班")
    private BigDecimal durationHours;
    @Schema(description = "请假原因，最多1000字，不放入流程变量")
    private String reason;
    @Schema(description = "绑定的流程编码，默认leave，由服务端配置")
    private String flowCode;
    @Schema(description = "关联实例ID，空值表示草稿；状态以引擎实例为准")
    private Long instanceId;
    @Schema(description = "乐观锁版本，编辑、提交和删除时必须携带")
    private Integer version;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "最后编辑或提交时间")
    private LocalDateTime updateTime;
}
