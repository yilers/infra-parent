package io.github.yilers.core.enums;

/**
 * 设备枚举
 * @author zhanghui
 * @since 2023/8/2 15:54
 */

public enum DeviceTypeEnum {

    WEB("web", "web端"),
    H5("h5", "h5端"),
    VX_MINI("vx", "微信小程序"),
    ALI_MINI("ali", "支付宝小程序"),
    ;

    private final String code;
    private final String desc;

    public String getCode() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }

    DeviceTypeEnum(final String code, final String desc) {
        this.code = code;
        this.desc = desc;
    }

}