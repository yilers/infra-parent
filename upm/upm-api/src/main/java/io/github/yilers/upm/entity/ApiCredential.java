package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 系统调用凭证。租户及审计字段由服务端维护。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upm_api_credential")
@Schema(description = "系统调用凭证")
public class ApiCredential extends BaseAllColumnDomain<ApiCredential> {
    @Schema(description = "凭证名称")
    private String name;

    @Schema(description = "全局唯一的公开调用标识")
    private String accessKey;

    @JsonIgnore
    @ToString.Exclude
    @Schema(description = "加密保存的调用密钥，禁止向列表和日志输出", hidden = true)
    private String secretCipher;

    @Schema(description = "失效时间，空表示长期有效")
    private java.time.LocalDateTime expireTime;

    @Schema(description = "每分钟允许调用次数")
    private Integer rateLimit;
}
