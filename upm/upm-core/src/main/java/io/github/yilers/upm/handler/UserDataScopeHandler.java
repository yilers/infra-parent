package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserDataScope;
import io.github.yilers.upm.i18n.UpmMessage;
import io.github.yilers.upm.request.UserDataScopeRequest;
import io.github.yilers.upm.service.UserDataScopeService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/** 用户数据权限管理入口；运行时读取本人权限仍由Service负责，避免管理授权与权限计算循环依赖。 */
@Component
@RequiredArgsConstructor
public class UserDataScopeHandler {
    private final UserService userService;
    private final UserDataScopeService userDataScopeService;
    private final CommonHandler commonHandler;

    public List<UserDataScope> findByUserId(Long userId) {
        checkTarget(userId, false);
        return userDataScopeService.findByUserId(userId);
    }

    public void bind(UserDataScopeRequest request) {
        checkTarget(request.getUserId(), true);
        userDataScopeService.bind(request);
    }

    private void checkTarget(Long userId, boolean writing) {
        User user = userId == null ? null : userService.getById(userId);
        Long tenantId = RequestContextHolder.getTenantId();
        if (user == null || tenantId == null || !Objects.equals(tenantId, user.getTenantId())) {
            throw new CommonException(UpmMessage.USER_TENANT_MISMATCH);
        }
        commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), user.getDeptId());
        if (writing && !CommonConst.YES.equals(user.getOperable())) {
            throw new CommonException(UpmMessage.USER_NOT_OPERABLE);
        }
    }
}
