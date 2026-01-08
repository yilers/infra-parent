package com.github.yilers.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataScopeEnum {

    ALL(1, "全部"),

    SELF_DEPT_AND_CHILD(2, "本部门及下属部门"),

    SELF_DEPT(3, "仅本部门"),

    CUSTOM(4, "自定义部门"),

    MY_SELF(5, "仅本人"),

    ;

    private final Integer value;
    private final String name;


}
