package io.github.yilers.upm.handler;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
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
        String key = StrUtil.format(CommonConst.USER_ROLE_ID_CACHE_KEY, loginId);
        List<Long> roleIdList = (List<Long>) SaManager.getSaTokenDao().getObject(key);
        if (roleIdList == null) {
            List<Role> roleList = userRoleService.findRoleListByUserId(loginId);
            roleIdList = roleList.stream().map(Role::getId).collect(Collectors.toList());
            SaManager.getSaTokenDao().setObject(key, roleIdList, CommonConst.KEY_EXPIRE);
        }
        return roleIdList;
    }

    @Override
    public List<String> getRoleCodeListByUserId(Long loginId) {
        String key = StrUtil.format(CommonConst.USER_ROLE_CODE_CACHE_KEY, loginId);
        List<String> roleCodeList = (List<String>) SaManager.getSaTokenDao().getObject(key);
        if (roleCodeList == null) {
            List<Role> roleList = userRoleService.findRoleListByUserId(loginId);
            roleCodeList = roleList.stream().map(Role::getRoleCode).collect(Collectors.toList());
            SaManager.getSaTokenDao().setObject(key, roleCodeList, CommonConst.KEY_EXPIRE);
        }
        return roleCodeList;
    }

    @Override
    public List<String> getPermissionList(Long loginId, String loginType) {
        List<Long> roleIdList = getRoleIdListByUserId(loginId);
        List<Permission> permissionList = rolePermissionService.findPermissionListByRoleIdList(roleIdList);
        return permissionList.stream().filter(permission -> permission.getDevice().equals(loginType))
                .map(Permission::getPermissionCode).collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionCodeListByRoleId(Long roleId) {
        String key = StrUtil.format(CommonConst.ROLE_PERMISSION_CACHE_KEY, roleId);
        List<String> permissionCodeList = (List<String>) SaManager.getSaTokenDao().getObject(key);
        if (permissionCodeList == null) {
            List<Permission> permissionList = rolePermissionService.findPermissionListByRoleId(roleId);
            permissionCodeList = permissionList.stream().map(Permission::getPermissionCode).collect(Collectors.toList());
            SaManager.getSaTokenDao().setObject(key, permissionCodeList, CommonConst.KEY_EXPIRE);
        }
        return permissionCodeList;
    }

    public void logout(Long userId) {
        // 清除缓存
        try {
            log.info("用户退出登录: {}", userId);
            Cache<Object, Object> userRole = cacheManager.getCache("userRole:");
            if (userRole != null) {
                boolean remove = userRole.remove(userId);
                log.info("清除用户角色缓存: {}", remove);
            }
            Cache<Object, Object> userDataScope = cacheManager.getCache("userDataScope:");
            if (userDataScope != null) {
                boolean remove = userDataScope.remove(userId);
                log.info("清除用户数据权限缓存: {}", remove);
            }
            Cache<Object, Object> user = cacheManager.getCache("user:");
            if (user != null) {
                boolean remove = user.remove(userId);
                log.info("清除单用户缓存: {}", remove);
            }
            Cache<Object, Object> userCurrentInfo = cacheManager.getCache("user:currentInfo:");
            if (userCurrentInfo != null) {
                boolean remove = userCurrentInfo.remove(userId);
                log.info("清除用户当前详细信息缓存: {}", remove);
            }
            String key = StrUtil.format(CommonConst.USER_ROLE_ID_CACHE_KEY, userId);
            SaManager.getSaTokenDao().delete(key);
            key = StrUtil.format(CommonConst.ROLE_PERMISSION_CACHE_KEY, userId);
            SaManager.getSaTokenDao().delete(key);
        } finally {
            StpUtil.logout(userId);
        }
    }
}
