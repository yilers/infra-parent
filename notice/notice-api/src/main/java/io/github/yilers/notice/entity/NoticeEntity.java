package io.github.yilers.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

/** 通知主记录公共字段；不复用启用、可操作字段，防止与发布状态混用。 */
@Data
public abstract class NoticeEntity {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "所属租户，由可信上下文设置")
    private Long tenantId;

    @Version
    @Schema(description = "乐观锁版本，编辑和发布时必须携带")
    private Integer version;

    @TableLogic
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    @Schema(description = "创建人ID")
    private Long createId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "最近更新时间")
    private LocalDateTime updateTime;
}
