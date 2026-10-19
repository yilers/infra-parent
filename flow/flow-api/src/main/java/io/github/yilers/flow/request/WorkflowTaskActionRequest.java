package io.github.yilers.flow.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 通用审批只接受意见，不接受浏览器覆盖租户、办理人、跳转目标或流程变量。
 * 需要修改业务参数的审批必须由相应业务接口校验后再调用WorkflowService。
 */
@Schema(description = "流程审批请求")
public record WorkflowTaskActionRequest(
        @Size(max = 500, message = "审批意见最多500个字符")
        @Schema(description = "审批意见，退回时必填，最多500个字符") String message) {
}
