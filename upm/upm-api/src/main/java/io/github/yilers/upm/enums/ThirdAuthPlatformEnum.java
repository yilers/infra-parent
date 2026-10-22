package io.github.yilers.upm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * UPM第三方认证平台目录。
 *
 * <p>平台编码会持久化到数据库和用户绑定关系中，因此不能直接使用第三方SDK中的枚举，
 * 避免SDK升级影响已保存的数据。目录中可以包含尚未实现接入的平台，但是否真正可用
 * 必须以 {@code ThirdAuthProviderRegistry} 中注册了对应实现为准。</p>
 */
@Getter
@RequiredArgsConstructor
public enum ThirdAuthPlatformEnum {

    DING_TALK("dingTalk", "钉钉", "DT"),
    FEI_SHU("feiShu", "飞书", "飞"),
    WE_COM("weCom", "企业微信", "企"),
    WE_CHAT("weChat", "微信", "微");

    /** 数据库存储的平台编码。 */
    private final String code;

    /** 页面展示的平台名称。 */
    private final String name;

    /** 平台目录卡片使用的短标识，避免将展示组件耦合到后端。 */
    private final String icon;

    /**
     * 判断平台编码是否登记在平台目录中。
     *
     * @param code 平台编码
     * @return 是否支持
     */
    public static boolean supports(String code) {
        return Arrays.stream(values()).anyMatch(platform -> platform.code.equals(code));
    }

    /**
     * 根据平台编码获取展示名称。
     *
     * @param code 平台编码
     * @return 平台名称，未知编码原样返回
     */
    public static String getName(String code) {
        return Arrays.stream(values())
                .filter(platform -> platform.code.equals(code))
                .map(ThirdAuthPlatformEnum::getName)
                .findFirst()
                .orElse(code);
    }
}
