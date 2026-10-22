package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 第三方认证平台目录及租户可用状态。
 *
 * <p>该响应不包含任何凭据。{@code implemented} 表示服务端已接入对应认证流程；
 * 登录页只应展示 {@code usable} 为 true 的平台。</p>
 */
@Schema(description = "第三方认证平台目录及可用状态")
public record ThirdAuthPlatformResponse(
        @Schema(description = "稳定的平台编码", example = "dingTalk") String platform,
        @Schema(description = "平台显示名称", example = "钉钉") String platformName,
        @Schema(description = "平台图标短标识", example = "DT") String icon,
        @Schema(description = "当前服务是否已实现该平台认证", example = "true") boolean implemented,
        @Schema(description = "当前租户是否已创建该平台配置", example = "true") boolean configured,
        @Schema(description = "当前租户是否已启用该平台", example = "true") boolean usable,
        @Schema(description = "配置记录ID，仅管理端目录返回") Long configId) {
}
