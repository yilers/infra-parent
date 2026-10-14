package io.github.yilers.upm.mapper;

import io.github.yilers.upm.entity.Dept;
import io.github.yilers.web.mybatis.CustomMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DeptMapper extends CustomMapper<Dept> {

    /** 锁定部门行，串行化同一部门主管设置。 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id FROM upm_dept WHERE id = #{deptId} AND tenant_id = #{tenantId} AND deleted = 0 FOR UPDATE")
    Long lockById(@Param("deptId") Long deptId, @Param("tenantId") Long tenantId);
}
