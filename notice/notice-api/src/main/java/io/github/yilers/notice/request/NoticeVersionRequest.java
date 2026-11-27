package io.github.yilers.notice.request;

import io.github.yilers.notice.model.*;
import io.swagger.v3.oas.annotations.media.Schema;

/** 带版本的写操作请求。 */
@Schema(description = "带版本的写操作请求。")
public record NoticeVersionRequest(
        @Schema(description = "记录ID，新建为空") Long id, @Schema(description = "乐观锁版本，修改必传") Integer version) {}
