package io.github.yilers.auth.api;

import java.util.Set;

/** 仅用于验签的内部凭证快照，不得序列化到响应或输出日志。 */
public record ApiCredentialIdentity(Long credentialId, Long tenantId, String secret,
                                    Set<String> permissions, int requestsPerMinute) {
    public ApiCredentialIdentity {
        permissions = Set.copyOf(permissions);
    }

    @Override
    public String toString() {
        return "ApiCredentialIdentity[credentialId=" + credentialId + ", tenantId=" + tenantId + "]";
    }
}
