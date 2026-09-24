package io.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.response.UserInfoResponse;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.web.mybatis.CustomMapper;
import io.github.yilers.web.permission.DataPermission;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends CustomMapper<User> {

    /**
     * 按账号查询当前有效租户下的用户。
     *
     * <p>租户采用逻辑删除，重新使用相同租户编码时，必须排除已删除租户遗留的同名账号。</p>
     */
    User findByAccount(@Param("account") String account);

    UserInfoResponse currentInfo(Long userId);

    @DataPermission(tableName = "upm_user")
    Page<UserInfoResponse> findByPage(@Param("page") Page<?> p,
                          @Param("request") BasePageRequest<UserPageRequest> request);
}
