package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用管理返回对象。密钥只返回配置状态，避免明文或密文离开服务端。
 */
@Data
@Schema(description = "应用管理信息，不返回SSO客户端密钥")
public class ApplicationResponse {
    @Schema(description = "应用ID", example = "1")
    private Long id;

    @Schema(description = "应用名称", example = "OA办公系统")
    private String name;

    @Schema(description = "租户内唯一的应用编码", example = "oa")
    private String code;

    @Schema(description = "完整SSO客户端标识，格式为“租户编码:应用编码”", example = "yilers.com:oa")
    private String clientId;

    @Schema(description = "应用菜单图标", example = "lucide:briefcase-business")
    private String icon;

    @Schema(description = "应用Logo地址", example = "https://oa.example.com/logo.png")
    private String logo;

    @Schema(description = "应用首页地址", example = "https://oa.example.com")
    private String homeUrl;

    @Schema(description = "是否启用SSO：1-启用，0-关闭", example = "1")
    private Integer ssoEnabled;

    @Schema(description = "允许的SSO回调地址列表")
    private List<String> redirectUris;

    @Schema(description = "业务后端接收Sa-Token签名单点退出通知的完整地址", example = "https://oa-api.example.com/sso/pushC")
    private String ssoPushUrl;

    @Schema(description = "是否已经生成SSO客户端密钥；仅返回状态，不返回密钥", example = "true")
    private Boolean secretConfigured;

    @Schema(description = "SSO客户端密钥最后生成时间")
    private LocalDateTime secretUpdateTime;

    @Schema(description = "应用描述")
    private String description;

    @Schema(description = "显示顺序，数值越小越靠前", example = "0")
    private Integer sortNumber;

    @Schema(description = "是否允许修改：1-允许，0-内置应用不可修改", example = "1")
    private Integer operable;

    @Schema(description = "是否启用：1-启用，0-停用", example = "1")
    private Integer usable;

    @Schema(description = "乐观锁版本号", example = "1")
    private Integer version;
}
