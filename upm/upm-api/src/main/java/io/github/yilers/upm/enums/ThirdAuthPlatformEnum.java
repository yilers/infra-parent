package io.github.yilers.upm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * UPM支持的第三方认证平台。
 *
 * <p>这里只登记已经纳入UPM接入计划的平台，不能直接使用第三方SDK中的枚举，
 * 避免SDK升级影响数据库中长期保存的平台编码。</p>
 */
@Getter
@RequiredArgsConstructor
public enum ThirdAuthPlatformEnum {

    DING_TALK("dingTalk", "钉钉");

    /** 数据库存储的平台编码。 */
    private final String code;

    /** 页面展示的平台名称。 */
    private final String name;

    /**
     * 判断平台编码是否受UPM支持。
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
