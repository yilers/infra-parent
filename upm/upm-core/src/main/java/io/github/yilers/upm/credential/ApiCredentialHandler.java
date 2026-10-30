package io.github.yilers.upm.credential;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yilers.upm.entity.ApiCredential;
import io.github.yilers.upm.entity.ApiCredentialPermission;
import io.github.yilers.upm.mapper.ApiCredentialMapper;
import io.github.yilers.upm.mapper.ApiCredentialPermissionMapper;
import io.github.yilers.upm.request.ApiCredentialRequest;
import io.github.yilers.upm.response.ApiCredentialSecretResponse;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.upm.i18n.UpmExtraMessage;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 凭证管理。修改权限与版本检查处于同一事务；首版不缓存凭证，停用立即生效。 */
@Component
@RequiredArgsConstructor
public class ApiCredentialHandler {
    private static final int INITIAL_VERSION = 1;
    private static final int NOT_DELETED = 0;
    private static final int OPERABLE = 1;
    private final ApiCredentialMapper mapper;
    private final ApiCredentialPermissionMapper permissionMapper;
    private final ApiPermissionCatalog catalog;
    private final ApiSecretCipher cipher;
    private final SecureRandom random = new SecureRandom();

    public List<ApiCredential> list() {
        requireTenant();
        return mapper.selectList(new LambdaQueryWrapper<ApiCredential>().orderByDesc(ApiCredential::getId));
    }

    public List<String> permissions(Long id) {
        requireTenant();
        if (mapper.selectById(id) == null) throw new CommonException(UpmExtraMessage.API_CREDENTIAL_NOT_FOUND);
        return permissionMapper.selectList(new LambdaQueryWrapper<ApiCredentialPermission>()
                .eq(ApiCredentialPermission::getCredentialId, id))
                .stream().map(ApiCredentialPermission::getPermissionCode).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredentialSecretResponse create(ApiCredentialRequest request) {
        validate(request);
        String secret = randomValue();
        ApiCredential value = new ApiCredential();
        value.setName(request.getName());
        value.setAccessKey(randomValue());
        value.setSecretCipher(cipher.encrypt(secret));
        value.setExpireTime(request.getExpireTime());
        value.setRateLimit(request.getRateLimit());
        value.setUsable(request.getUsable());
        value.setVersion(INITIAL_VERSION);
        value.setDeleted(NOT_DELETED);
        value.setOperable(OPERABLE);
        mapper.insert(value);
        replacePermissions(value.getId(), request.getPermissions());
        return new ApiCredentialSecretResponse(value.getId(), value.getAccessKey(), secret);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ApiCredentialRequest request) {
        validate(request);
        if (request.getId() == null || request.getVersion() == null) throw new CommonException(UpmExtraMessage.API_ID_VERSION_REQUIRED);
        ApiCredential value = mapper.selectById(request.getId());
        if (value == null) throw new CommonException(UpmExtraMessage.API_CREDENTIAL_NOT_FOUND);
        if (!request.getVersion().equals(value.getVersion())) throw new CommonException(UpmExtraMessage.API_CONFLICT);
        // 使用显式更新以允许清空expireTime；版本条件不可省略。
        int count = mapper.updateConfiguration(value.getId(), value.getTenantId(), request.getVersion(),
                request.getName(), request.getExpireTime(), request.getRateLimit(), request.getUsable());
        if (count != 1) throw new CommonException(UpmExtraMessage.API_CONFLICT);
        replacePermissions(value.getId(), request.getPermissions());
    }

    /**
     * 保留原有Key及授权，生成新的Secret；提交后旧密钥不再用于新的验签请求。
     * 已在重置前完成验签的并发请求可能继续执行。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiCredentialSecretResponse resetSecret(Long id, Integer version) {
        Long tenantId = requireTenant();
        if (id == null || version == null) {
            throw new CommonException(UpmExtraMessage.API_ID_VERSION_REQUIRED);
        }
        ApiCredential value = mapper.selectById(id);
        if (value == null) {
            throw new CommonException(UpmExtraMessage.API_CREDENTIAL_NOT_FOUND);
        }
        if (!version.equals(value.getVersion())) {
            throw new CommonException(UpmExtraMessage.API_CONFLICT);
        }
        String secret = randomValue();
        String secretCipher = cipher.encrypt(secret);
        if (mapper.resetSecret(id, tenantId, version, secretCipher) != 1) {
            throw new CommonException(UpmExtraMessage.API_CONFLICT);
        }
        return new ApiCredentialSecretResponse(id, value.getAccessKey(), secret);
    }

    private void replacePermissions(Long id, Set<String> permissions) {
        // 授权关系物理替换，不使用逻辑删除，否则重新授权会与唯一索引冲突。
        permissionMapper.deleteForCredential(id, requireTenant());
        for (String code : permissions) {
            ApiCredentialPermission grant = new ApiCredentialPermission();
            grant.setCredentialId(id);
            grant.setPermissionCode(code);
            grant.setVersion(INITIAL_VERSION);
            grant.setUsable(OPERABLE);
            grant.setOperable(OPERABLE);
            grant.setDeleted(NOT_DELETED);
            permissionMapper.insert(grant);
        }
    }

    private void validate(ApiCredentialRequest request) {
        requireTenant();
        if (request.getExpireTime() != null && !request.getExpireTime().isAfter(LocalDateTime.now())) {
            throw new CommonException(UpmExtraMessage.API_EXPIRATION_INVALID);
        }
        Set<String> allowed = catalog.list().stream().map(ApiPermissionCatalog.Capability::code).collect(Collectors.toSet());
        if (!allowed.containsAll(request.getPermissions())) throw new CommonException(UpmExtraMessage.API_PERMISSION_UNKNOWN);
    }

    private Long requireTenant() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) throw new CommonException(UpmExtraMessage.API_TENANT_REQUIRED);
        return tenantId;
    }

    private String randomValue() {
        byte[] value = new byte[32];
        random.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
