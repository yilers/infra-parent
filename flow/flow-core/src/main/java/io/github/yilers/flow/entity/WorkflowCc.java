package io.github.yilers.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 节点通过时形成的抄送接收快照，接收人及姓名不随组织关系变化。 */
@Data
@TableName("flow_cc")
public class WorkflowCc {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String tenantId;
    private Long instanceId;
    private Long sourceTaskId;
    private String nodeCode;
    private String nodeName;
    private String recipientId;
    private String recipientName;
    private Date createTime;
    private Date readTime;
}
