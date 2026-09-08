package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Application;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.ApplicationService;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 接口鉴权使用服务端配置的应用，不信任客户端提交的appId。
 */
@Component
@RequiredArgsConstructor
public class ApplicationAccessHandler {
    private final ApplicationService applicationService;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;

    @Value("${upm.application-code:infra}")
    private String applicationCode;

    public Application currentApplication() {
        Application application = applicationService.getOne(Wrappers.<Application>lambdaQuery()
                .eq(Application::getTenantId, RequestContextHolder.getTenantId())
                .eq(Application::getCode, applicationCode));
        if (application == null || !CommonConst.YES.equals(application.getUsable())) {
            throw new CommonException("应用不存在或已停用");
        }
        return application;
    }

    public User currentUser() {
        // 登录态中的用户id是可信来源；此处只为定位用户所属租户。
        try {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            User user = userService.getById(StpUtil.getLoginIdAsLong());
            if (user == null || !CommonConst.YES.equals(user.getUsable())) {
                throw new CommonException("账号暂不可用");
            }
            return user;
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    public List<Permission> currentPermissions(Long userId) {
        Long previousTenantId = RequestContextHolder.getTenantId();
        User user = currentUser();
        try {
            RequestContextHolder.setTenantId(user.getTenantId());
            return findPermissions(userId);
        } finally {
            RequestContextHolder.setTenantId(previousTenantId);
        }
    }

    private List<Permission> findPermissions(Long userId) {
        Application application = currentApplication();
        List<Long> roleIds = userRoleService.findRoleListByUserId(userId).stream()
                .filter(role -> CommonConst.YES.equals(role.getUsable()))
                .map(Role::getId).toList();
        return rolePermissionService.findPermissionListByRoleIdList(roleIds).stream()
                .filter(permission -> application.getId().equals(permission.getAppId()))
                .filter(permission -> CommonConst.YES.equals(permission.getUsable()))
                .filter(permission -> StpUtil.getLoginDeviceType().equals(permission.getDevice()))
                .toList();
    }
}
