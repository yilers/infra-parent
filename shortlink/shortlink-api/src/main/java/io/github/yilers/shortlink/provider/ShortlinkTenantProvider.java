package io.github.yilers.shortlink.provider;

/** 公开跳转时校验域名所属租户状态，供单体或独立部署实现。 */
public interface ShortlinkTenantProvider {
    boolean isUsable(Long tenantId);
}
