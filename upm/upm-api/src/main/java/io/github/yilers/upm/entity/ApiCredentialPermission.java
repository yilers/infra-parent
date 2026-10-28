package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 系统调用接口授权。租户及审计字段由服务端维护。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upm_api_credential_permission")
@Schema(description = "系统调用接口授权")
public class ApiCredentialPermission extends BaseAllColumnDomain<ApiCredentialPermission> {
    @Schema(description = "调用凭证ID")
    private Long credentialId;

    @Schema(description = "接口能力编码，不依赖接口路径")
    private String permissionCode;
}
