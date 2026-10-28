package io.github.yilers.shortlink.mapper;

import org.apache.ibatis.annotations.*;

/** 显式租户条件与插件共同约束，号段更新必须在独立事务中提交后才能使用。 */
public interface ShortlinkSequenceMapper {
    @Insert("""
        INSERT INTO shortlink_sequence(namespace_id,tenant_id,max_allocated_id,step)
        VALUES(#{namespaceId},#{tenantId},0,#{step})
        """)
    int initialize(Long namespaceId, Long tenantId, int step);

    @Select("""
        SELECT max_allocated_id FROM shortlink_sequence
        WHERE namespace_id=#{namespaceId} AND tenant_id=#{tenantId} FOR UPDATE
        """)
    Long lockHighWatermark(Long namespaceId, Long tenantId);

    @Update("""
        UPDATE shortlink_sequence SET max_allocated_id=#{maximum},update_time=CURRENT_TIMESTAMP
        WHERE namespace_id=#{namespaceId} AND tenant_id=#{tenantId} AND max_allocated_id=#{previous}
        """)
    int advance(Long namespaceId, Long tenantId, long previous, long maximum);
}
