package io.github.yilers.upm.request;

import io.github.yilers.api.base.BaseOperateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用客户端密钥生成或重置请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "应用客户端密钥生成或重置请求")
public class ApplicationSecretResetRequest extends BaseOperateRequest {
    @NotNull(message = "version不能为空")
    @Schema(description = "当前应用乐观锁版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer version;
}
