package io.github.yilers.upm.mapper;

import io.github.yilers.upm.entity.ApiCredentialPermission;
import io.github.yilers.web.mybatis.CustomMapper;

/** 系统调用接口授权数据访问。默认保留租户过滤。 */
public interface ApiCredentialPermissionMapper extends CustomMapper<ApiCredentialPermission> {
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    @org.apache.ibatis.annotations.Select("""
        SELECT permission_code FROM upm_api_credential_permission
        WHERE credential_id=#{credentialId} AND tenant_id=#{tenantId} AND deleted=0 AND usable=1
        """)
    java.util.List<String> findForAuthentication(Long credentialId, Long tenantId);

    @org.apache.ibatis.annotations.Delete("""
        DELETE FROM upm_api_credential_permission WHERE credential_id=#{credentialId} AND tenant_id=#{tenantId}
        """)
    int deleteForCredential(Long credentialId, Long tenantId);
}
