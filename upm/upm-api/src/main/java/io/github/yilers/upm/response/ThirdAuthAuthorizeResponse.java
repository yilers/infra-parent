package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 第三方认证授权地址。
 */
@Schema(description = "第三方认证授权地址")
public record ThirdAuthAuthorizeResponse(
        @Schema(description = "第三方平台OAuth2授权地址") String authorizeUrl) {
}
