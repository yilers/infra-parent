package io.github.yilers.upm.request;

import io.github.yilers.api.field.FieldViewMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** 按角色版本整体保存当前可管理字段，范围外的已有授权由服务端保留。 */
@Schema(description = "角色字段权限保存请求")
public record RoleFieldRequest(
        @NotNull @Schema(description = "目标角色ID") Long roleId,
        @NotNull @Schema(description = "读取配置时的角色版本") Integer version,
        @NotNull @Size(max = 1000) @Valid @Schema(description = "字段授权列表，未提交项不修改") List<@NotNull Grant> fields) {
    @Schema(description = "单个字段授权")
    public record Grant(@NotNull @Schema(description = "字段目录ID") Long fieldId,
                        @NotNull @Schema(description = "HIDDEN/MASKED/FULL") FieldViewMode viewMode,
                        @NotNull @Schema(description = "是否允许修改，须完整可见") Boolean editable) { }
}
