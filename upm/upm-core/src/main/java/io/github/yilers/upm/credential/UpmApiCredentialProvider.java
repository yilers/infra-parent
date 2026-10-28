package io.github.yilers.upm.credential;

import io.github.yilers.auth.api.ApiCredentialIdentity;
import io.github.yilers.auth.api.ApiCredentialProvider;
import io.github.yilers.upm.entity.ApiCredential;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.mapper.ApiCredentialMapper;
import io.github.yilers.upm.mapper.ApiCredentialPermissionMapper;
import io.github.yilers.upm.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;

/** 按全局Key定位凭证，再检查租户。跨租户查询仅限专用Mapper方法，不影响普通管理查询。 */
@Component
@RequiredArgsConstructor
public class UpmApiCredentialProvider implements ApiCredentialProvider {
    private static final int ENABLED = 1;
    private final ApiCredentialMapper credentialMapper;
    private final ApiCredentialPermissionMapper permissionMapper;
    private final TenantMapper tenantMapper;
    private final ApiSecretCipher cipher;

    @Override
    public boolean belongsToTenant(Long credentialId, Long tenantId) {
        ApiCredential value = credentialMapper.selectById(credentialId);
        return value != null && value.getTenantId().equals(tenantId);
    }

    @Override
    public ApiCredentialIdentity findByAccessKey(String accessKey) {
        ApiCredential credential = credentialMapper.findForAuthentication(accessKey);
        if (credential == null || !Integer.valueOf(ENABLED).equals(credential.getUsable())
                || credential.getExpireTime() != null && !credential.getExpireTime().isAfter(LocalDateTime.now())) {
            return null;
        }
        Tenant tenant = tenantMapper.selectById(credential.getTenantId());
        if (tenant == null || !Integer.valueOf(ENABLED).equals(tenant.getUsable())) {
            return null;
        }
        return new ApiCredentialIdentity(credential.getId(), credential.getTenantId(),
                cipher.decrypt(credential.getSecretCipher()),
                new HashSet<>(permissionMapper.findForAuthentication(credential.getId(), credential.getTenantId())),
                credential.getRateLimit());
    }
}
