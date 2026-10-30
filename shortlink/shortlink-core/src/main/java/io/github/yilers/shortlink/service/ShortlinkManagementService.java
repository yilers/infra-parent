package io.github.yilers.shortlink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.github.yilers.auth.api.ApiCredentialProvider;
import io.github.yilers.shortlink.entity.*;
import io.github.yilers.shortlink.mapper.*;
import io.github.yilers.shortlink.request.*;
import io.github.yilers.shortlink.support.NamespaceKeyCipher;
import io.github.yilers.shortlink.i18n.ShortlinkMessage;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.IDN;
import java.util.*;

import static io.github.yilers.shortlink.support.ShortlinkRules.*;

/** 管理配置不接受客户端审计字段，固定标识不可编辑，旧短链永远使用生成时目标快照。 */
@Service
@RequiredArgsConstructor
public class ShortlinkManagementService {
    private static final Set<String> RESERVED_PATHS = Set.of("auth", "system", "public", "api", "sso", "error", "actuator", "shortlink", "flow");
    private final ShortlinkConfigMapper configs;
    private final ShortlinkDomainMapper domains;
    private final ShortlinkNamespaceMapper namespaces;
    private final ShortlinkSequenceMapper sequences;
    private final ShortlinkConfigGrantMapper grants;
    private final ObjectProvider<ApiCredentialProvider> credentials;
    private final NamespaceKeyCipher namespaceKeys;

    public List<ShortlinkConfig> configs() {
        ShortlinkServiceImpl.tenant();
        return configs.selectList(new LambdaQueryWrapper<ShortlinkConfig>().orderByDesc(ShortlinkConfig::getId));
    }

    public List<ShortlinkDomain> domains() {
        ShortlinkServiceImpl.tenant();
        return domains.selectList(new LambdaQueryWrapper<ShortlinkDomain>().orderByDesc(ShortlinkDomain::getId));
    }

    public List<ShortlinkNamespace> namespaces(Long configId) {
        ShortlinkServiceImpl.tenant();
        return namespaces.selectList(new LambdaQueryWrapper<ShortlinkNamespace>()
                .eq(ShortlinkNamespace::getConfigId, configId).orderByAsc(ShortlinkNamespace::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveConfig(ShortlinkConfigRequest request) {
        ShortlinkServiceImpl.tenant();
        if (TEMPLATE.equals(request.getTargetType())) {
            if (request.getUrlTemplate() == null || !request.getUrlTemplate().contains("{code}")) throw new CommonException(ShortlinkMessage.TEMPLATE_CODE_REQUIRED);
            validateUrl(request.getUrlTemplate().replace("{code}", "example"));
        } else if (request.getUrlTemplate() != null && !request.getUrlTemplate().isBlank()) {
            throw new CommonException(ShortlinkMessage.URL_TEMPLATE_FORBIDDEN);
        }
        ShortlinkConfig value = request.getId() == null ? new ShortlinkConfig() : configs.selectById(request.getId());
        if (value == null) throw new CommonException(ShortlinkMessage.CONFIG_NOT_FOUND);
        if (request.getId() != null) {
            checkVersion(value, request.getVersion());
            if (!Objects.equals(value.getCode(), request.getCode()) || !Objects.equals(value.getTargetType(), request.getTargetType())) {
                throw new CommonException(ShortlinkMessage.CONFIG_IMMUTABLE);
            }
        }
        value.setName(request.getName());
        value.setCode(request.getCode());
        value.setTargetType(request.getTargetType());
        value.setUrlTemplate(request.getUrlTemplate());
        value.setDefaultValidSeconds(request.getDefaultValidSeconds());
        value.setUsable(request.getUsable());
        if (request.getId() == null) {
            initialize(value);
            configs.insert(value);
        } else if (configs.updateConfiguration(value.getId(), ShortlinkServiceImpl.tenant(), request.getVersion(),
                value.getName(), value.getUrlTemplate(), value.getDefaultValidSeconds(), value.getUsable()) != 1) {
            throw new CommonException(ShortlinkMessage.DATA_CONFLICT);
        }
        return value.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveDomain(ShortlinkDomainRequest request) {
        ShortlinkServiceImpl.tenant();
        String host;
        try {
            host = IDN.toASCII(request.getHost().trim()).toLowerCase(Locale.ROOT);
            if (!host.matches("(?=.{1,253}$)[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")
                    || host.contains("..") || host.endsWith(".")) throw new IllegalArgumentException();
            validateUrl(request.getScheme() + "://" + host);
        } catch (Exception e) {
            throw new CommonException(ShortlinkMessage.DOMAIN_FORMAT_INVALID);
        }
        ShortlinkDomain value = request.getId() == null ? new ShortlinkDomain() : domains.selectById(request.getId());
        if (value == null) throw new CommonException(ShortlinkMessage.DOMAIN_NOT_FOUND);
        if (request.getId() != null) {
            checkVersion(value, request.getVersion());
            if (!Objects.equals(value.getHost(), host) || !Objects.equals(value.getScheme(), request.getScheme())
                    || !Objects.equals(value.getPublicPort(), request.getPublicPort())) {
                throw new CommonException(ShortlinkMessage.DOMAIN_IMMUTABLE);
            }
        }
        value.setHost(host);
        value.setScheme(request.getScheme());
        value.setPublicPort(request.getPublicPort());
        value.setUsable(request.getUsable());
        if (request.getId() == null) {
            initialize(value);
            domains.insert(value);
        } else if (domains.updateById(value) != 1) throw new CommonException(ShortlinkMessage.DATA_CONFLICT);
        return value.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveNamespace(ShortlinkNamespaceRequest request) {
        Long tenant = ShortlinkServiceImpl.tenant();
        capacity(request.getCodeLength());
        if (RESERVED_PATHS.contains(request.getPathCode())) throw new CommonException(ShortlinkMessage.PATH_RESERVED);
        if (configs.selectById(request.getConfigId()) == null || domains.selectById(request.getDomainId()) == null) {
            throw new CommonException(ShortlinkMessage.CONFIG_DOMAIN_TENANT);
        }
        ShortlinkNamespace value = request.getId() == null ? new ShortlinkNamespace() : namespaces.selectById(request.getId());
        if (value == null) throw new CommonException(ShortlinkMessage.NAMESPACE_NOT_FOUND);
        if (request.getId() != null) {
            checkVersion(value, request.getVersion());
            if (!Objects.equals(value.getConfigId(), request.getConfigId()) || !Objects.equals(value.getDomainId(), request.getDomainId())
                    || !Objects.equals(value.getPathCode(), request.getPathCode()) || !Objects.equals(value.getGenerateType(), request.getGenerateType())
                    || !Objects.equals(value.getCodeLength(), request.getCodeLength())) throw new CommonException(ShortlinkMessage.NAMESPACE_IMMUTABLE);
        }
        value.setConfigId(request.getConfigId());
        value.setDomainId(request.getDomainId());
        value.setPathCode(request.getPathCode());
        value.setGenerateType(request.getGenerateType());
        value.setCodeLength(request.getCodeLength());
        value.setUsable(request.getUsable());
        if (request.getId() == null) {
            initialize(value);
            if (AUTO.equals(value.getGenerateType())) {
                value.setCodeAlgorithm(ALGORITHM_FF1_V1);
                value.setCodeKeyCipher(namespaceKeys.createEncryptedKey());
            } else {
                value.setCodeAlgorithm(ALGORITHM_NONE);
            }
            namespaces.insert(value);
            if (AUTO.equals(value.getGenerateType())) sequences.initialize(value.getId(), tenant, SEGMENT_SIZE);
        } else if (namespaces.updateById(value) != 1) throw new CommonException(ShortlinkMessage.DATA_CONFLICT);
        return value.getId();
    }

    public List<Long> grants(Long configId) {
        ShortlinkServiceImpl.tenant();
        if (configs.selectById(configId) == null) throw new CommonException(ShortlinkMessage.SHORTLINK_CONFIG_NOT_FOUND);
        return grants.selectList(new LambdaQueryWrapper<ShortlinkConfigGrant>().eq(ShortlinkConfigGrant::getConfigId, configId))
                .stream().map(ShortlinkConfigGrant::getCredentialId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void grant(Long configId, Integer version, Set<Long> credentialIds) {
        Long tenant = ShortlinkServiceImpl.tenant();
        ShortlinkConfig config = configs.selectById(configId);
        if (config == null) throw new CommonException(ShortlinkMessage.SHORTLINK_CONFIG_NOT_FOUND);
        checkVersion(config, version);
        for (Long id : credentialIds) {
            if (!credentials.getObject().belongsToTenant(id, tenant)) throw new CommonException(ShortlinkMessage.CREDENTIAL_TENANT);
        }
        // 授权修改也递增配置版本，阻止并发编辑互相覆盖。
        if (configs.updateById(config) != 1) throw new CommonException(ShortlinkMessage.DATA_CONFLICT);
        grants.deleteForConfig(configId, tenant);
        for (Long id : credentialIds) {
            ShortlinkConfigGrant value = new ShortlinkConfigGrant();
            initialize(value);
            value.setConfigId(configId);
            value.setCredentialId(id);
            grants.insert(value);
        }
    }

    private static void checkVersion(BaseAllColumnDomain<?> value, Integer version) {
        if (version == null || !version.equals(value.getVersion())) throw new CommonException(ShortlinkMessage.VERSION_CONFLICT);
    }

    private static void initialize(BaseAllColumnDomain<?> value) {
        value.setVersion(INITIAL_VERSION);
        value.setOperable(ENABLED);
        value.setDeleted(DISABLED);
        if (value.getUsable() == null) value.setUsable(ENABLED);
    }
}
