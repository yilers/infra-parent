package io.github.yilers.shortlink.mapper;

import io.github.yilers.shortlink.entity.ShortlinkConfig;
import io.github.yilers.web.mybatis.CustomMapper;

/** 短链配置数据访问。默认保留租户过滤。 */
public interface ShortlinkConfigMapper extends CustomMapper<ShortlinkConfig> {
    @org.apache.ibatis.annotations.Select("""
        SELECT id FROM shortlink_config WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted=0 FOR UPDATE
        """)
    Long lockConfiguration(Long id, Long tenantId);

    @org.apache.ibatis.annotations.Update("""
        UPDATE shortlink_config SET name=#{name},url_template=#{urlTemplate},default_valid_seconds=#{defaultValidSeconds},
        usable=#{usable},version=version+1,update_time=CURRENT_TIMESTAMP
        WHERE id=#{id} AND tenant_id=#{tenantId} AND version=#{version} AND deleted=0
        """)
    int updateConfiguration(Long id, Long tenantId, Integer version, String name, String urlTemplate,
                            Long defaultValidSeconds, Integer usable);
}
