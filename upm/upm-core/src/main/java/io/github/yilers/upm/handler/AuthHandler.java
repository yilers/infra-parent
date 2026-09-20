package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.sso.template.SaSsoServerTemplate;
import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.hutool.v7.crypto.digest.BCrypt;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import io.github.yilers.auth.AuthService;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.LoginRequest;
import io.github.yilers.upm.response.LoginResponse;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
@SuppressWarnings("all")
public class AuthHandler implements AuthService {
    private final UserService userService;
    private final TenantService tenantService;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;
    private final CacheManager cacheManager;
    private final ApplicationAccessHandler applicationAccessHandler;
    private final SaSsoServerTemplate ssoServerTemplate;

    public LoginResponse login(LoginRequest loginRequest) {
        String account = loginRequest.getAccount();
        String password = loginRequest.getPassword();
        String device = loginRequest.getDevice();
        User user = userService.findByAccount(account);
        if (ObjUtil.isEmpty(user)) {
            throw new CommonException("账号或密码错误");
        } else {
            Tenant tenant = tenantService.getById(user.getTenantId());
            if (ObjUtil.isEmpty(tenant) || CommonConst.NO.equals(tenant.getUsable())) {
                throw new CommonException("租户已禁用");
            }
            if (BCrypt.checkpw(password, user.getPassword())) {
                Integer usable = user.getUsable();
                if (ObjUtil.isEmpty(usable) || CommonConst.NO.equals(usable)) {
                    throw new CommonException("账号暂不可用");
                }
                Long previousTenantId = RequestContextHolder.getTenantId();
                try {
                    RequestContextHolder.setTenantId(user.getTenantId());
                    applicationAccessHandler.currentApplication();
                } finally {
                    RequestContextHolder.setTenantId(previousTenantId);
                }
                StpUtil.login(user.getId(), device);
                SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
                LoginResponse response = BeanUtil.copyProperties(tokenInfo, LoginResponse.class);
                response.setName(user.getName());
                return response;
            } else {
                throw new CommonException("账号或密码错误");
            }
        }

    }

    @Override
    public List<Long> getRoleIdListByUserId(Long loginId) {
        return userRoleService.findRoleListByUserId(loginId).stream()
                .map(Role::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleCodeListByUserId(Long loginId) {
        return userRoleService.findRoleListByUserId(loginId).stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionList(Long loginId, String loginType) {
        return applicationAccessHandler.currentPermissions(loginId).stream()
                .map(Permission::getPermissionCode).filter(StrUtil::isNotBlank).distinct().toList();
    }

    @Override
    public List<String> getPermissionCodeListByRoleId(Long roleId) {
        Long appId = applicationAccessHandler.currentApplication().getId();
        return rolePermissionService.findPermissionListByRoleId(roleId).stream()
                .filter(permission -> appId.equals(permission.getAppId()))
                .filter(permission -> CommonConst.YES.equals(permission.getUsable()))
                .filter(permission -> StpUtil.getLoginDeviceType().equals(permission.getDevice()))
                .map(Permission::getPermissionCode).filter(StrUtil::isNotBlank).distinct().toList();
    }

    public void logout(Long userId) {
        // 清除缓存
        try {
            log.info("用户退出登录: {}", userId);
            Cache<Object, Object> userRole = cacheManager.getCache(CommonConst.USER_ROLE_CACHE_NAME);
            if (userRole != null) {
                boolean remove = userRole.remove(userId);
                log.info("清除用户角色缓存: {}", remove);
            }
            Cache<Object, Object> userDataScope = cacheManager.getCache(CommonConst.USER_DATA_SCOPE_CACHE_NAME);
            if (userDataScope != null) {
                boolean remove = userDataScope.remove(userId);
                log.info("清除用户数据权限缓存: {}", remove);
            }
            Cache<Object, Object> user = cacheManager.getCache(CommonConst.USER_CACHE_NAME);
            if (user != null) {
                boolean remove = user.remove(userId);
                log.info("清除单用户缓存: {}", remove);
            }
            Cache<Object, Object> userCurrentInfo = cacheManager.getCache(CommonConst.USER_CURRENT_INFO_CACHE_NAME);
            if (userCurrentInfo != null) {
                boolean remove = userCurrentInfo.remove(userId);
                log.info("清除用户当前详细信息缓存: {}", remove);
            }
        } finally {
            try {
                // 同时注销认证中心会话，并由 Sa-Token 向已登记的业务应用推送单点注销。
                ssoServerTemplate.ssoLogout(userId);
            } catch (Exception e) {
                // 业务应用通知失败不能阻断 UPM 自身退出，保留原有本地注销作为兜底。
                log.warn("SSO客户端注销通知失败，继续注销UPM本地会话，用户: {}", userId, e);
                StpUtil.logout(userId);
            }
        }
    }
}
