package io.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.response.UserInfoResponse;
import io.github.yilers.web.base.BasePageRequest;
import io.github.yilers.web.mybatis.CustomMapper;
import io.github.yilers.web.permission.DataPermission;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends CustomMapper<User> {

    UserInfoResponse currentInfo(Long userId);

    @DataPermission(tableName = "upm_user")
    Page<UserInfoResponse> findByPage(@Param("page") Page<?> p,
                          @Param("request") BasePageRequest<UserPageRequest> request);
}
