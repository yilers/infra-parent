package io.github.yilers.shortlink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 短链记录。租户及审计字段由服务端维护。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shortlink_link")
@Schema(description = "短链记录")
public class ShortlinkLink extends BaseAllColumnDomain<ShortlinkLink> {
    @Schema(description = "短链配置ID")
    private Long configId;

    @Schema(description = "路径配置ID")
    private Long namespaceId;

    @Schema(description = "区分大小写的Base62短码")
    private String shortCode;

    @Schema(description = "生成时解析的目标地址快照")
    private String targetUrl;

    @Schema(description = "调用方业务编码，不是配置编码")
    private String businessCode;

    @Schema(description = "批量创建的批次标识，单条创建为空")
    private String batchId;

    @Schema(description = "调用方提供的批次内业务标识")
    private String businessId;

    @Schema(description = "配置内唯一的创建幂等标识")
    private String requestId;

    @Schema(description = "请求参数SHA256摘要，用于检查幂等冲突")
    private String requestHash;

    @Schema(description = "短链失效时间，空表示永久")
    private java.time.LocalDateTime expireTime;

    @Schema(description = "系统调用创建时的凭证ID，与用户创建人分开")
    private Long credentialId;
}
