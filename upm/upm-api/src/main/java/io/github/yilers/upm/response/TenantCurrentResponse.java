package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 登录后页面初始化所需的租户展示信息，不返回管理状态或任意扩展内容。 */
@Schema(description = "当前登录租户展示信息")
public record TenantCurrentResponse(
        @Schema(description = "当前登录态所属租户ID") Long id,
        @Schema(description = "租户名称") String name,
        @Schema(description = "租户编码") String code,
        @Schema(description = "系统显示名称，未配置时使用租户名称") String title,
        @Schema(description = "租户Logo，未配置时为空，由前端使用默认图标") String logo) { }
