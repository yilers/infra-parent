package com.github.yilers.auth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.text.StrUtil;
import com.github.yilers.core.constant.CommonConst;
import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 权限数据加载源
 * @author zhanghui
 * @date 2023/8/2 10:08
 */

@SuppressWarnings("all")
public class StpInterfaceImpl implements StpInterface {

    /**
     * 这里采用业务和权限缓存分离 注入直接用的是独立redis
     */
    @Resource
    private AuthService authService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // user --> role --> permission
        // 如果一旦角色修改，只需要修改角色对应的权限一条缓存 防止雪崩
        List<Long> roleIdList = findRoleIdListByUserId(Long.parseLong(loginId.toString()));
        List<String> permissionCodeList = new ArrayList<>();
        for (Long roleId : roleIdList) {
            String key = cn.hutool.v7.core.text.StrUtil.format(CommonConst.ROLE_PERMISSION_CACHE_KEY, roleId);
            List<String> itemPermissionCodeList = (List<String>) SaManager.getSaTokenDao().getObject(key);
            if (itemPermissionCodeList == null) {
                itemPermissionCodeList = authService.getPermissionCodeListByRoleId(roleId);
            }
            permissionCodeList.addAll(itemPermissionCodeList);
        }
        return permissionCodeList;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roleCodeList = authService.getRoleCodeListByUserId(Long.parseLong(loginId.toString()));
        if (CollUtil.isEmpty(roleCodeList)) {
            return Collections.emptyList();
        }
        return roleCodeList;
    }

    private List<Long> findRoleIdListByUserId(Object loginId) {
        String key = StrUtil.format(CommonConst.USER_ROLE_ID_CACHE_KEY, loginId);
        List<Long> roleIdList = (List<Long>) SaManager.getSaTokenDao().getObject(key);
        if(roleIdList == null) {
            roleIdList = authService.getRoleIdListByUserId(Long.parseLong(loginId.toString()));
        }
        return roleIdList;
    }

}
