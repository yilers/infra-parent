package io.github.yilers.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlatformEnum {

    WX("wx", "wx"),
    DING_TALK("dingTalk", "钉钉"),
    ;

    private final String code;
    private final String desc;
}
