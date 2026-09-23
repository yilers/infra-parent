package io.github.yilers.upm.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.yilers.api.validated.Add;
import io.github.yilers.api.validated.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 第三方认证平台配置新增或修改请求。
 */
@Data
@Schema(description = "第三方认证平台配置新增或修改请求")
public class ThirdAuthConfigRequest {

    @NotNull(groups = Update.class, message = "配置id不能为空")
    @Schema(description = "配置ID，修改时必填", example = "1")
    private Long id;

    @NotBlank(message = "认证平台不能为空")
    @Size(max = 30)
    @Schema(description = "第三方认证平台编码，创建后不可修改",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "dingTalk")
    private String platform;

    @NotBlank(message = "Client ID不能为空")
    @Size(max = 255)
    @Schema(description = "第三方平台Client ID或AppKey",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "dingxxxxxxxx")
    private String clientId;

    @NotBlank(groups = Add.class, message = "Client Secret不能为空")
    @Size(max = 512)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "第三方平台Client Secret或AppSecret；修改时留空表示保留原值",
            accessMode = Schema.AccessMode.WRITE_ONLY, example = "secret-value")
    private String clientSecret;

    @NotBlank(message = "授权回调地址不能为空")
    @Size(max = 500)
    @Schema(description = "第三方平台授权完成后的UPM回调地址",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://upm.example.com/auth/third/dingTalk/callback")
    private String redirectUri;

    @Size(max = 20, message = "授权范围最多配置20个")
    @Schema(description = "第三方平台授权范围列表")
    private List<@Size(max = 100) String> scopes = new ArrayList<>();

    @Size(max = 500)
    @Schema(description = "配置说明")
    private String description;

    @NotNull(groups = Update.class, message = "版本号不能为空")
    @Schema(description = "乐观锁版本号，修改时必填", example = "1")
    private Integer version;
}
