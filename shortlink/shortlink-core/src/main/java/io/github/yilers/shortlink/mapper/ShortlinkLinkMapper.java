package io.github.yilers.shortlink.mapper;

import io.github.yilers.shortlink.entity.ShortlinkLink;
import io.github.yilers.web.mybatis.CustomMapper;

/** 短链记录数据访问。默认保留租户过滤。 */
public interface ShortlinkLinkMapper extends CustomMapper<ShortlinkLink> {
    @org.apache.ibatis.annotations.Select("""
        SELECT COUNT(*) FROM shortlink_link WHERE config_id=#{configId} AND tenant_id=#{tenantId} AND request_id=#{requestId}
        """)
    long requestExists(Long configId, Long tenantId, String requestId);

    @org.apache.ibatis.annotations.Select("""
        SELECT COUNT(*) FROM shortlink_link WHERE namespace_id=#{namespaceId} AND tenant_id=#{tenantId} AND short_code=#{code}
        """)
    long codeExists(Long namespaceId, Long tenantId, String code);

    @org.apache.ibatis.annotations.Update("""
        UPDATE shortlink_link SET expire_time=#{expireTime},version=version+1,update_time=CURRENT_TIMESTAMP
        WHERE id=#{id} AND tenant_id=#{tenantId} AND version=#{version} AND deleted=0
        """)
    int renew(Long id, Long tenantId, Integer version, java.time.LocalDateTime expireTime);

    @org.apache.ibatis.annotations.Update("""
        UPDATE shortlink_link SET deleted=1,version=version+1,update_time=CURRENT_TIMESTAMP
        WHERE id=#{id} AND tenant_id=#{tenantId} AND version=#{version} AND deleted=0
        """)
    int markDeleted(Long id, Long tenantId, Integer version);
}
