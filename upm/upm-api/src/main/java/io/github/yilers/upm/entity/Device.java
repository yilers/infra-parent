package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.github.yilers.web.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upm_device")
public class Device extends BaseDomain<Device> {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private String description;
    private String expand;
    private Integer operable;
    private Integer usable;
    @Version
    private Integer version;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
}
