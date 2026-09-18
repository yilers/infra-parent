package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("upm_application")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户应用")
public class Application extends BaseAllColumnDomain<Application> {
    @Schema(description = "应用名称", example = "OA办公系统")
    private String name;

    @Schema(description = "租户内唯一的应用编码，用于组成SSO客户端标识，创建后不可修改", example = "oa")
    private String code;

    @Schema(description = "应用菜单图标，支持前端图标编码", example = "lucide:briefcase-business")
    private String icon;

    @Schema(description = "应用Logo地址", example = "https://oa.example.com/logo.png")
    private String logo;

    @Schema(description = "应用首页地址", example = "https://oa.example.com")
    private String homeUrl;

    @Schema(description = "是否启用SSO：1-启用，0-关闭", example = "0")
    private Integer ssoEnabled;

    @JsonIgnore
    @Schema(hidden = true)
    private String ssoSecret;

    @Schema(description = "允许的SSO回调地址，数据库中以英文逗号分隔")
    private String redirectUris;

    @Schema(description = "业务后端接收Sa-Token签名单点退出通知的完整地址", example = "https://oa-api.example.com/sso/pushC")
    private String ssoPushUrl;

    @Schema(description = "SSO客户端密钥最后生成时间")
    private LocalDateTime secretUpdateTime;

    @Schema(description = "应用描述")
    private String description;

    @Schema(description = "显示顺序，数值越小越靠前", example = "0")
    private Integer sortNumber;
}
