package com.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yilers.upm.entity.Log;
import com.github.yilers.upm.response.LogInfoResponse;
import com.github.yilers.web.base.BasePageRequest;
import com.github.yilers.web.mybatis.CustomMapper;
import com.github.yilers.web.permission.DataPermission;
import org.apache.ibatis.annotations.Param;

public interface LogMapper extends CustomMapper<Log> {

    @InterceptorIgnore(tenantLine = "true", dataPermission = "false")
    @DataPermission(tableName = "upm_log", deptField = "dept_id", userField = "operator")
    Page<LogInfoResponse> findByPage(@Param("page") Page<?> p,
                                     @Param("request") BasePageRequest<Log> request);

}
