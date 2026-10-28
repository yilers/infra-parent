package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 仅在创建或重置凭证时返回一次；不得写入审计日志。 */
@Schema(description = "创建或重置凭证结果，secret仅此次返回")
public record ApiCredentialSecretResponse(
        @Schema(description = "凭证ID") Long id,
        @Schema(description = "公开调用标识") String accessKey,
        @Schema(description = "调用密钥，请由调用方安全保存") String secret) {
    @Override
    public String toString() {
        return "ApiCredentialSecretResponse[id=" + id + "]";
    }
}
