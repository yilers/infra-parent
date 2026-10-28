package io.github.yilers.shortlink.mapper;

import io.github.yilers.shortlink.entity.ShortlinkConfigGrant;
import io.github.yilers.web.mybatis.CustomMapper;

/** 短链配置调用授权数据访问。默认保留租户过滤。 */
public interface ShortlinkConfigGrantMapper extends CustomMapper<ShortlinkConfigGrant> {
    @org.apache.ibatis.annotations.Delete("DELETE FROM shortlink_config_grant WHERE config_id=#{id} AND tenant_id=#{tenantId}")
    int deleteForConfig(Long id, Long tenantId);
}
