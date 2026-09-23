package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 第三方认证平台配置返回对象。
 *
 * <p>Client Secret只返回是否已经配置，任何查询接口都不返回原文。</p>
 */
@Data
@Schema(description = "第三方认证平台配置信息，不返回Client Secret")
public class ThirdAuthConfigResponse {

    @Schema(description = "配置ID", example = "1")
    private Long id;

    @Schema(description = "第三方认证平台编码", example = "dingTalk")
    private String platform;

    @Schema(description = "第三方认证平台名称", example = "钉钉")
    private String platformName;

    @Schema(description = "第三方平台Client ID或AppKey", example = "dingxxxxxxxx")
    private String clientId;

    @Schema(description = "是否已经配置Client Secret；仅返回状态，不返回原文", example = "true")
    private Boolean secretConfigured;

    @Schema(description = "第三方平台授权回调地址")
    private String redirectUri;

    @Schema(description = "第三方平台授权范围列表")
    private List<String> scopes;

    @Schema(description = "配置说明")
    private String description;

    @Schema(description = "是否允许修改：1-允许，0-内置配置不可修改", example = "1")
    private Integer operable;

    @Schema(description = "是否启用：1-启用，0-停用", example = "0")
    private Integer usable;

    @Schema(description = "乐观锁版本号", example = "1")
    private Integer version;
}
