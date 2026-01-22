package io.github.yilers.web.base;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseAllColumnDomain<T extends Model<?>> extends Model<T> {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @Version
    @Schema(description = "乐观锁")
    private Integer version;

    @Schema(description = "是否可用 1-可用 0-不可用")
    private Integer usable;

    @Schema(description = "是否可操作 1-是 0-否")
    private Integer operable;

    @TableLogic
    @Schema(description = "逻辑删除 是否删除 1-删除 0-未删除")
    private Integer deleted;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField(value = "create_id", fill = FieldFill.INSERT)
    private Long createId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}

