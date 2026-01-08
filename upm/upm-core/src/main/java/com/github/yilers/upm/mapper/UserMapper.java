package com.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yilers.upm.entity.User;
import com.github.yilers.upm.request.UserPageRequest;
import com.github.yilers.upm.response.UserInfoResponse;
import com.github.yilers.web.base.BasePageRequest;
import com.github.yilers.web.mybatis.CustomMapper;
import com.github.yilers.web.permission.DataPermission;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends CustomMapper<User> {

    UserInfoResponse currentInfo(Long userId);

    @DataPermission(tableName = "upm_user")
    Page<UserInfoResponse> findByPage(@Param("page") Page<?> p,
                          @Param("request") BasePageRequest<UserPageRequest> request);
}
