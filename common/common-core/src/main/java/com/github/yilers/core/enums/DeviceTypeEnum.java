package com.github.yilers.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备枚举
 * @author zhanghui
 * @date 2023/8/2 15:54
 */

@Getter
@AllArgsConstructor
public enum DeviceTypeEnum {

    WEB("web", "web端"),
    H5("h5", "h5端"),
    VX_MINI("vx", "微信小程序"),
    ALI_MINI("ali", "支付宝小程序"),
    ;

    private final String code;
    private final String desc;

}