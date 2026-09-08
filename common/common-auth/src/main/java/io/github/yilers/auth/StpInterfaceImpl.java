package io.github.yilers.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.v7.core.collection.CollUtil;
import jakarta.annotation.Resource;

import java.util.Collections;
import java.util.List;

/**
 * 权限数据加载源
 * @author zhanghui
 * @since 2023/8/2 10:08
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
        // 具体服务决定权限作用域及缓存策略，不能在此复用跨应用的权限编码缓存。
        return authService.getPermissionList(Long.parseLong(loginId.toString()), loginType);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roleCodeList = authService.getRoleCodeListByUserId(Long.parseLong(loginId.toString()));
        if (CollUtil.isEmpty(roleCodeList)) {
            return Collections.emptyList();
        }
        return roleCodeList;
    }

}
