package com.github.yilers.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserTypeEnum {

    ADMIN("admin", "管理端"),
    USER("user", "用户端")

    ;

    private final String code;
    private final String desc;
}
