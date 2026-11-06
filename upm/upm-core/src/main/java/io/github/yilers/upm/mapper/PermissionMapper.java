package io.github.yilers.upm.mapper;

import io.github.yilers.upm.entity.Permission;
import io.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PermissionMapper extends CustomMapper<Permission> {

    List<Permission> findPermissionsByUserId(@Param("userId") Long userId,
                                             @Param("device") String device,
                                             @Param("appId") Long appId);

    /** 查询用户通过有效角色持有的、可用于新增授权的权限。 */
    List<Permission> findGrantablePermissions(@Param("userId") Long userId,
                                               @Param("device") String device,
                                               @Param("appId") Long appId,
                                               @Param("usable") Integer usable,
                                               @Param("notDeleted") Integer notDeleted);
}
