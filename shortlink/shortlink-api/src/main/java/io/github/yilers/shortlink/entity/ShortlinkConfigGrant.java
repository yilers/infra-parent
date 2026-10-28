package io.github.yilers.shortlink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 短链配置调用授权。租户及审计字段由服务端维护。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shortlink_config_grant")
@Schema(description = "短链配置调用授权")
public class ShortlinkConfigGrant extends BaseAllColumnDomain<ShortlinkConfigGrant> {
    @Schema(description = "短链配置ID")
    private Long configId;

    @Schema(description = "同租户系统调用凭证ID")
    private Long credentialId;
}
