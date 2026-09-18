package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.sso.template.SaSsoServerTemplate;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONUtil;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.SsoAuthorizeRequest;
import io.github.yilers.upm.response.SsoApplicationResponse;
import io.github.yilers.upm.response.SsoAuthorizeResponse;
import io.github.yilers.upm.response.SsoLoginContextResponse;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.upm.sso.SsoClientResolver;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SsoHandler {
    private final SsoClientResolver clientResolver;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;
    private final CommonHandler commonHandler;
    private final SaSsoServerTemplate ssoServerTemplate;

    public SsoApplicationResponse findApplication(String clientId) {
        return toApplicationResponse(clientResolver.requireEnabled(clientResolver.resolve(clientId)));
    }

    public SsoAuthorizeResponse authorize(SsoAuthorizeRequest request) {
        SsoClientResolver.Client client = clientResolver.requireEnabled(clientResolver.resolve(request.getClient()));
        User user = findUser(StpUtil.getLoginIdAsLong());
        if (!client.tenant().getId().equals(user.getTenantId())) {
            throw new CommonException("当前账号无权访问该租户应用");
        }
        String device = StpUtil.getLoginDeviceType();
        List<Permission> permissions = findPermissions(client, user.getId(), device);
        if (permissions.isEmpty()) {
            throw new CommonException("当前账号没有该应用的访问权限");
        }
        String redirectUrl = ssoServerTemplate.buildRedirectUrl(request.getClient(), request.getRedirect(),
                user.getId(), StpUtil.getTokenValue());
        if (StrUtil.isNotBlank(request.getState())) {
            redirectUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                    .queryParam("state", request.getState()).build().encode().toUriString();
        }
        return new SsoAuthorizeResponse(redirectUrl);
    }

    public SsoLoginContextResponse buildLoginContext(Long userId, String clientId, String tokenValue) {
        SsoClientResolver.Client client = clientResolver.requireEnabled(clientResolver.resolve(clientId));
        User user = findUser(userId);
        if (!client.tenant().getId().equals(user.getTenantId())) {
            throw new CommonException("用户与应用不属于同一租户");
        }
        String device = StpUtil.getStpLogic().getLoginDeviceTypeByToken(tokenValue);
        Long previousTenantId = RequestContextHolder.getTenantId();
        try {
            RequestContextHolder.setTenantId(user.getTenantId());
            List<Role> roles = userRoleService.findRoleListByUserId(userId).stream()
                    .filter(role -> CommonConst.YES.equals(role.getUsable())).toList();
            List<Permission> permissions = filterPermissions(client, roles, device);
            if (permissions.isEmpty()) {
                throw new CommonException("用户没有该应用的访问权限");
            }
            SsoLoginContextResponse response = new SsoLoginContextResponse();
            response.setApplication(toApplicationResponse(client));
            response.setUser(new SsoLoginContextResponse.User(user.getId(), user.getAccount(), user.getNickname(),
                    user.getName(), user.getPhoto(), user.getDeptId(), user.getPositionId()));
            response.setRoles(roles.stream().map(role -> new SsoLoginContextResponse.Role(role.getId(),
                    role.getRoleCode(), role.getRoleName(), role.getDataScope())).toList());
            response.setMenus(permissions);
            response.setPermissions(permissions.stream().map(Permission::getPermissionCode)
                    .filter(StrUtil::isNotBlank).distinct().toList());
            response.setDataScope(commonHandler.judgeDataScope(userId, roles));
            return response;
        } finally {
            RequestContextHolder.setTenantId(previousTenantId);
        }
    }

    private List<Permission> findPermissions(SsoClientResolver.Client client, Long userId, String device) {
        Long previousTenantId = RequestContextHolder.getTenantId();
        try {
            RequestContextHolder.setTenantId(client.tenant().getId());
            List<Role> roles = userRoleService.findRoleListByUserId(userId).stream()
                    .filter(role -> CommonConst.YES.equals(role.getUsable())).toList();
            return filterPermissions(client, roles, device);
        } finally {
            RequestContextHolder.setTenantId(previousTenantId);
        }
    }

    private List<Permission> filterPermissions(SsoClientResolver.Client client, List<Role> roles, String device) {
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        return rolePermissionService.findPermissionListByRoleIdList(roleIds).stream()
                .filter(permission -> client.application().getId().equals(permission.getAppId()))
                .filter(permission -> CommonConst.YES.equals(permission.getUsable()))
                .filter(permission -> device.equals(permission.getDevice()))
                .toList();
    }

    private User findUser(Long userId) {
        try {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            User user = userService.getById(userId);
            if (user == null || !CommonConst.YES.equals(user.getUsable())) {
                throw new CommonException("账号不存在或已停用");
            }
            return user;
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    private SsoApplicationResponse toApplicationResponse(SsoClientResolver.Client client) {
        SsoApplicationResponse response = new SsoApplicationResponse();
        response.setClientId(client.clientId());
        response.setTenantCode(client.tenant().getCode());
        response.setTenantName(client.tenant().getName());
        response.setTenantLogo(readTenantLogo(client.tenant().getExpand()));
        response.setApplicationCode(client.application().getCode());
        response.setApplicationName(client.application().getName());
        response.setApplicationLogo(client.application().getLogo());
        response.setHomeUrl(client.application().getHomeUrl());
        return response;
    }

    private String readTenantLogo(String expand) {
        if (StrUtil.isBlank(expand)) {
            return null;
        }
        try {
            return JSONUtil.parseObj(expand).getStr("logo");
        } catch (Exception ignored) {
            return null;
        }
    }
}
