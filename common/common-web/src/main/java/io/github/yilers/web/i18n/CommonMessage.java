package io.github.yilers.web.i18n;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 公共模块可复用的用户提示。 */
@Getter
@RequiredArgsConstructor
public enum CommonMessage implements MessageCode {

    /** 通用服务异常；是否用于未知异常由调用方决定。 */
    SERVICE_UNAVAILABLE("common.service.unavailable", "服务异常，请稍后重试"),
    SAVE_FAILED("common.save.failed", "保存失败"),
    UPDATE_CONFLICT("common.update.conflict", "更新失败 数据已经变更");

    private final String key;
    private final String defaultMessage;
}
