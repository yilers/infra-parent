package io.github.yilers.auth;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DefaultAuthServiceImpl implements AuthService {
    @Override
    public List<Long> getRoleIdListByUserId(Long loginId) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleCodeListByUserId(Long loginId) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getPermissionList(Long loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getPermissionCodeListByRoleId(Long roleId) {
        return Collections.emptyList();
    }
}
