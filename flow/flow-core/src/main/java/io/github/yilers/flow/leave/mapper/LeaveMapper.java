package io.github.yilers.flow.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.flow.leave.entity.LeaveApplication;
import io.github.yilers.flow.leave.model.LeaveInfo;
import io.github.yilers.flow.leave.request.LeaveSaveRequest;
import org.apache.ibatis.annotations.Param;

/** 请假业务只查询本租户记录，版本写入由显式条件保证，不跳过租户插件。 */
public interface LeaveMapper extends BaseMapper<LeaveApplication> {

    Page<LeaveInfo> pageMine(Page<LeaveInfo> page, @Param("tenantId") String tenantId,
                             @Param("userId") String userId, @Param("status") String status);

    LeaveInfo info(@Param("id") Long id, @Param("tenantId") String tenantId);

    LeaveApplication lockMine(@Param("id") Long id, @Param("tenantId") String tenantId,
                               @Param("userId") String userId);

    int editDraft(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("userId") String userId,
                   @Param("request") LeaveSaveRequest request);

    int bindInstance(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("userId") String userId,
                      @Param("version") Integer version, @Param("instanceId") Long instanceId);

    int deleteDraft(@Param("id") Long id, @Param("tenantId") String tenantId, @Param("userId") String userId,
                     @Param("version") Integer version);

    Long idByInstance(@Param("instanceId") Long instanceId, @Param("tenantId") String tenantId);
}
