package io.github.yilers.core.enums;


public enum DataScopeEnum {

    ALL(1, "全部"),

    SELF_DEPT_AND_CHILD(2, "本部门及下属部门"),

    SELF_DEPT(3, "仅本部门"),

    CUSTOM(4, "自定义部门"),

    MY_SELF(5, "仅本人"),

    ;

    private final Integer value;
    private final String name;

    public Integer getValue() {
        return this.value;
    }

    public String getDesc() {
        return this.name;
    }

    DataScopeEnum(final Integer value, final String name) {
        this.value = value;
        this.name = name;
    }

}
