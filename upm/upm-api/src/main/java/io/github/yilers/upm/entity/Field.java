package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 按租户隔离的业务字段目录，不是数据库列定义。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upm_field")
public class Field extends BaseAllColumnDomain<Field> {
    @Schema(description = "业务资源编码")
    private String resourceCode;
    @Schema(description = "业务资源名称")
    private String resourceName;
    @Schema(description = "业务字段编码")
    private String fieldCode;
    @Schema(description = "字段展示名称")
    private String fieldName;
    @Schema(description = "脱敏方式NONE/PHONE/EMAIL")
    private String maskType;
    @Schema(description = "是否允许配置编辑权限")
    private Boolean writeSupported;
    @Schema(description = "排序")
    private Integer sortNumber;
}
