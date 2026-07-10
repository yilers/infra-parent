package io.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@InterceptorIgnore(tenantLine = "true")
public interface TenantMapper extends CustomMapper<Tenant> {

    @Select("""
            SELECT id, name, code, description, expand, operable, usable,
                version, deleted, create_time, update_time
            FROM upm_tenant
            """)
    List<Tenant> findAll();
}
