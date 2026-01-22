package io.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.upm.entity.Log;
import io.github.yilers.upm.response.LogInfoResponse;
import io.github.yilers.web.base.BasePageRequest;
import io.github.yilers.web.mybatis.CustomMapper;
import io.github.yilers.web.permission.DataPermission;
import org.apache.ibatis.annotations.Param;

public interface LogMapper extends CustomMapper<Log> {

    @InterceptorIgnore(tenantLine = "true", dataPermission = "false")
    @DataPermission(tableName = "upm_log", deptField = "dept_id", userField = "operator")
    Page<LogInfoResponse> findByPage(@Param("page") Page<?> p,
                                     @Param("request") BasePageRequest<Log> request);

}
