package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 应用密钥重置结果。secret 仅在本次响应中返回，服务端不提供再次查询能力。
 */
@Schema(description = "SSO客户端密钥生成或重置结果；密钥明文仅返回一次")
public record ApplicationSecretResponse(
        @Schema(description = "完整SSO客户端标识", example = "yilers.com:oa") String clientId,
        @Schema(description = "新生成的客户端密钥，关闭页面后无法再次查询",
                example = "QGFqV80F0vyQyhzkvkafXTVB1mNKd0cSo6P4Ut-9-Ow") String secret) {
}
