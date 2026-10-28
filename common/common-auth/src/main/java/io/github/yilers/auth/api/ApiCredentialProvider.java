package io.github.yilers.auth.api;

/** 凭证查询扩展点。实现必须校验凭证及其租户状态，不得信任客户端租户ID。 */
public interface ApiCredentialProvider {
    ApiCredentialIdentity findByAccessKey(String accessKey);

    /** 管理授权时校验目标凭证归属；默认拒绝，远程实现须明确提供此能力。 */
    default boolean belongsToTenant(Long credentialId, Long tenantId) {
        return false;
    }
}
