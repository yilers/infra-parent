package io.github.yilers.core.enums;

public enum UserTypeEnum {

    ADMIN("admin", "管理端"),
    USER("user", "用户端")

    ;

    private final String code;
    private final String desc;

    public String getCode() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }

    UserTypeEnum(final String code, final String desc) {
        this.code = code;
        this.desc = desc;
    }

}
