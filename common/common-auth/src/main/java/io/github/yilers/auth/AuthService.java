package io.github.yilers.auth;

import java.util.List;

public interface AuthService {

    List<Long> getRoleIdListByUserId(Long loginId);

    List<String> getRoleCodeListByUserId(Long loginId);

    List<String> getPermissionList(Long loginId, String loginType);

    List<String> getPermissionCodeListByRoleId(Long roleId);

}
