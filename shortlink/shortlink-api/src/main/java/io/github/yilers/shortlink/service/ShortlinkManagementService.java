package io.github.yilers.shortlink.service;

import io.github.yilers.shortlink.entity.ShortlinkConfig;
import io.github.yilers.shortlink.entity.ShortlinkDomain;
import io.github.yilers.shortlink.entity.ShortlinkNamespace;
import io.github.yilers.shortlink.request.ShortlinkConfigRequest;
import io.github.yilers.shortlink.request.ShortlinkDomainRequest;
import io.github.yilers.shortlink.request.ShortlinkNamespaceRequest;

import java.util.List;
import java.util.Set;

/** 当前租户的短链配置、域名、命名空间和凭证授权管理。 */
public interface ShortlinkManagementService {
    /** 列出当前租户的短链配置。 */
    List<ShortlinkConfig> configs();

    /** 列出当前租户的短链域名。 */
    List<ShortlinkDomain> domains();

    /** 查询指定配置下的命名空间。 */
    List<ShortlinkNamespace> namespaces(Long configId);

    /** 新建或按版本修改短链业务配置。 */
    Long saveConfig(ShortlinkConfigRequest request);

    /** 新建或按版本修改短链域名。 */
    Long saveDomain(ShortlinkDomainRequest request);

    /** 新建或按版本修改命名空间。 */
    Long saveNamespace(ShortlinkNamespaceRequest request);

    /** 查询可以调用指定短链配置的凭证ID。 */
    List<Long> grants(Long configId);

    /** 按配置版本替换短链业务的凭证授权。 */
    void grant(Long configId, Integer version, Set<Long> credentialIds);
}
