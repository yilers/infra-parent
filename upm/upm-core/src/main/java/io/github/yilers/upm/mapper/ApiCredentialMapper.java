package io.github.yilers.upm.mapper;

import io.github.yilers.upm.entity.ApiCredential;
import io.github.yilers.web.mybatis.CustomMapper;

/** 系统调用凭证数据访问。默认保留租户过滤。 */
public interface ApiCredentialMapper extends CustomMapper<ApiCredential> {
    /** 仅认证入口按全局唯一Key定位租户，不能用于管理端查询。 */
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    @org.apache.ibatis.annotations.Select("SELECT * FROM upm_api_credential WHERE access_key = #{accessKey} AND deleted = 0")
    ApiCredential findForAuthentication(String accessKey);

    @org.apache.ibatis.annotations.Update("""
        UPDATE upm_api_credential SET name=#{name}, expire_time=#{expireTime}, rate_limit=#{rateLimit},
        usable=#{usable}, version=version+1, update_time=CURRENT_TIMESTAMP
        WHERE id=#{id} AND tenant_id=#{tenantId} AND version=#{version} AND deleted=0
        """)
    int updateConfiguration(Long id, Long tenantId, Integer version, String name,
                            java.time.LocalDateTime expireTime, Integer rateLimit, Integer usable);

    /** 密钥重置只更新密文及版本，不改变调用方标识和现有授权。 */
    @org.apache.ibatis.annotations.Update("""
        UPDATE upm_api_credential SET secret_cipher=#{secretCipher}, version=version+1, update_time=CURRENT_TIMESTAMP
        WHERE id=#{id} AND tenant_id=#{tenantId} AND version=#{version} AND deleted=0
        """)
    int resetSecret(Long id, Long tenantId, Integer version, String secretCipher);
}
