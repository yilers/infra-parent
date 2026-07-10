package io.github.yilers.upm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import io.github.yilers.upm.entity.Device;
import io.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DeviceMapper extends CustomMapper<Device> {

    @Select("""
            SELECT
                id, name, code, description, expand, operable, usable,
                version, tenant_id, deleted, create_time, update_time
            FROM upm_device
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<Device> findAll();
}
