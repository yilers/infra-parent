package io.github.yilers.flow.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** 业务关联只提供可信类型及单据ID，不把正文或客户端跳转URL塞入流程变量。 */
@Schema(description = "流程关联业务入口")
public record WorkflowBusinessReference(@Schema(description = "受支持的业务类型，例如leave") String type,
                                         @Schema(description = "业务单据ID") Long id,
                                         @Schema(description = "业务名称") String title) {
}
