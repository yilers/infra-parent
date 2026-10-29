package io.github.yilers.upm.i18n;

import io.github.yilers.web.i18n.MessageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** UPM 模块的用户提示。 */
@Getter
@RequiredArgsConstructor
public enum UpmMessage implements MessageCode {

    USER_NOT_FOUND("upm.user.not-found", "用户不存在");

    private final String key;
    private final String defaultMessage;

    @Override
    public String getBasename() {
        return "i18n/upm-messages";
    }
}
