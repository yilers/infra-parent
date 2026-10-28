package io.github.yilers.shortlink.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yilers.shortlink.entity.*;
import io.github.yilers.shortlink.mapper.*;
import io.github.yilers.shortlink.provider.ShortlinkTenantProvider;
import io.github.yilers.shortlink.support.ShortlinkCache;
import io.github.yilers.web.context.RequestContextHolder;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 按域名解析公开短链。只跳转，不由服务器抓取目标URL。
 * 不信任X-Tenant-Id、X-Forwarded-Host；部署代理必须保留经过校验的Host。
 */
@RestController
@RequiredArgsConstructor
public class ShortlinkRedirectController {
    private static final int ENABLED = 1;
    private final ShortlinkDomainMapper domains;
    private final ShortlinkNamespaceMapper namespaces;
    private final ShortlinkConfigMapper configs;
    private final ShortlinkCache cache;
    private final ObjectProvider<ShortlinkTenantProvider> tenants;

    @SaIgnore
    @io.github.yilers.auth.api.PublicTenantEndpoint
    @GetMapping("/{path:[a-z][a-z0-9]{0,15}}/{code:[0-9A-Za-z]{6,8}}")
    public void redirect(@PathVariable String path, @PathVariable String code,
                         HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        // 公开入口即使附带用户token，也只按已登记域名确定租户，不能沿用用户租户。
        ShortlinkDomain domain = domains.findForRedirect(request.getServerName().toLowerCase(Locale.ROOT));
        ShortlinkTenantProvider tenantProvider = tenants.getIfAvailable();
        if (domain == null || tenantProvider == null || !tenantProvider.isUsable(domain.getTenantId())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        RequestContextHolder.setUserId(null);
        RequestContextHolder.setTenantId(domain.getTenantId());
        ShortlinkNamespace namespace = namespaces.selectOne(new LambdaQueryWrapper<ShortlinkNamespace>()
                .eq(ShortlinkNamespace::getDomainId, domain.getId()).eq(ShortlinkNamespace::getPathCode, path)
                .eq(ShortlinkNamespace::getUsable, ENABLED));
        if (namespace == null || code.length() != namespace.getCodeLength()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ShortlinkConfig config = configs.selectById(namespace.getConfigId());
        if (config == null || !Integer.valueOf(ENABLED).equals(config.getUsable())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ShortlinkCache.Target target = cache.find(namespace.getId(), code);
        if (target == null || !Integer.valueOf(ENABLED).equals(target.usable())
                || target.expireTime() != null && !target.expireTime().isAfter(LocalDateTime.now())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", target.url());
    }
}
