package io.github.yilers.upm.request;

import io.github.yilers.api.base.BaseOperateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 重置系统调用凭证的密钥，必须提交当前版本以防止并发覆盖。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统调用凭证密钥重置请求")
public class ApiCredentialSecretResetRequest extends BaseOperateRequest {
    @NotNull(message = "version不能为空")
    @Schema(description = "凭证当前乐观锁版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
}
