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
    UPDATE_CONFLICT("common.update.conflict", "更新失败 数据已经变更"),
    ID_REQUIRED("common.validation.id-required", "id不能为空"),
    VALIDATION_NOT_NULL("common.validation.not-null", "不能为null"),
    VALIDATION_NOT_BLANK("common.validation.not-blank", "不能为空"),
    VALIDATION_NOT_EMPTY("common.validation.not-empty", "不能为空"),
    VALIDATION_SIZE("common.validation.size", "长度需要在{0}和{1}之间"),
    VALIDATION_MIN("common.validation.min", "不能小于{0}"),
    VALIDATION_MAX("common.validation.max", "不能大于{0}"),
    VALIDATION_POSITIVE("common.validation.positive", "必须大于0"),
    VALIDATION_PATTERN("common.validation.pattern", "需要匹配\"{0}\"");

    private final String key;
    private final String defaultMessage;
}
