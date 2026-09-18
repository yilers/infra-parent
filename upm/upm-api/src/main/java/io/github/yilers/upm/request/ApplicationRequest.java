package io.github.yilers.upm.request;

import io.github.yilers.api.validated.Add;
import io.github.yilers.api.validated.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "应用新增或修改请求")
public class ApplicationRequest {
    @NotNull(groups = Update.class, message = "应用id不能为空")
    @Schema(description = "应用ID，修改时必填", example = "1")
    private Long id;

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 100)
    @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "OA办公系统")
    private String name;

    @NotBlank(groups = Add.class, message = "应用编码不能为空")
    @Pattern(regexp = "[a-z][a-z0-9_-]{0,63}", message = "应用编码须以小写字母开头，仅支持小写字母、数字、下划线和短横线")
    @Schema(description = "租户内唯一的应用编码，创建后不可修改；只填写应用部分，不包含租户编码",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "oa")
    private String code;

    @Size(max = 255)
    @Schema(description = "应用菜单图标，支持前端图标编码", example = "lucide:briefcase-business")
    private String icon;

    @Size(max = 255)
    @Schema(description = "应用Logo地址", example = "https://oa.example.com/logo.png")
    private String logo;

    @Size(max = 500)
    @Schema(description = "应用首页地址", example = "https://oa.example.com")
    private String homeUrl;

    @Min(0)
    @Max(1)
    @Schema(description = "是否启用SSO：1-启用，0-关闭；启用前必须生成密钥并配置回调及推送地址",
            allowableValues = {"0", "1"}, example = "0")
    private Integer ssoEnabled = 0;

    @Size(max = 20, message = "回调地址最多配置20个")
    @Schema(description = "允许的SSO回调地址列表，必须与授权请求中的redirect精确匹配，最多20个",
            example = "[\"https://oa.example.com/sso/callback\"]")
    private List<@Size(max = 500) String> redirectUris = new ArrayList<>();

    @Size(max = 500)
    @Schema(description = "业务后端接收Sa-Token签名单点退出通知的完整地址，必须能被UPM服务端访问",
            example = "https://oa-api.example.com/sso/pushC")
    private String ssoPushUrl;

    @Size(max = 500)
    @Schema(description = "应用描述")
    private String description;

    @NotNull
    @Min(0)
    @Schema(description = "显示顺序，数值越小越靠前", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer sortNumber = 0;

    @NotNull(groups = Update.class, message = "版本号不能为空")
    @Schema(description = "乐观锁版本号，修改时必填", example = "1")
    private Integer version;
}
