package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户第三方认证平台配置。
 *
 * <p>该配置用于UPM作为认证中心调用钉钉等外部身份平台，和业务应用的SSO客户端配置相互独立。</p>
 */
@Data
@TableName("upm_third_auth_config")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户第三方认证平台配置")
public class ThirdAuthConfig extends BaseAllColumnDomain<ThirdAuthConfig> {

    @Schema(description = "第三方认证平台编码", example = "dingTalk")
    private String platform;

    @Schema(description = "第三方平台客户端ID", example = "dingxxxxxxxx")
    private String clientId;

    /**
     * 第一阶段按照需求明文保存，但禁止通过实体序列化、列表或详情接口返回。
     */
    @JsonIgnore
    @Schema(hidden = true)
    private String clientSecret;

    @Schema(description = "第三方平台授权完成后返回的UPM个人中心地址",
            example = "https://upm.example.com/third-auth-callback.html")
    private String redirectUri;

    @Schema(description = "授权范围，数据库中以英文逗号分隔")
    private String scopes;

    @Schema(description = "配置说明")
    private String description;
}
