package io.github.yilers.upm.response;

import io.github.yilers.api.field.FieldMaskType;
import io.github.yilers.api.field.FieldViewMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 角色字段配置及当前操作者的可授权范围，范围外配置由服务端保留。 */
@Schema(description = "角色字段权限与当前操作者可授权上限")
public record RoleFieldResponse(
        @Schema(description = "角色乐观锁版本") Integer version,
        @Schema(description = "字段目录与策略") List<Row> fields) {

    @Schema(description = "字段授权项；grantable为false时只读，不可通过手工请求修改")
    public record Row(
            @Schema(description = "字段目录ID") Long fieldId,
            @Schema(description = "业务资源编码") String resourceCode,
            @Schema(description = "业务资源名称") String resourceName,
            @Schema(description = "业务字段编码") String fieldCode,
            @Schema(description = "业务字段名称") String fieldName,
            @Schema(description = "代码声明的脱敏方式") FieldMaskType maskType,
            @Schema(description = "代码是否支持配置修改权限") boolean writeSupported,
            @Schema(description = "目标角色的查看方式") FieldViewMode viewMode,
            @Schema(description = "目标角色是否允许修改") boolean editable,
            @Schema(description = "当前人可授予的最高查看级别") FieldViewMode maxViewMode,
            @Schema(description = "当前人是否可以管理本项配置") boolean grantable,
            @Schema(description = "当前人是否能授予修改权限") boolean grantEditable) { }
}
