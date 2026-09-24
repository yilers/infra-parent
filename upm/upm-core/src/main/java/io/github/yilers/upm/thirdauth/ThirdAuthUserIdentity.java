package io.github.yilers.upm.thirdauth;

/**
 * 第三方认证平台返回的用户公开身份信息。
 *
 * @param openId 平台应用内用户标识
 * @param unionId 平台开发者范围内用户标识
 * @param nickname 用户昵称
 * @param avatarUrl 用户头像
 */
public record ThirdAuthUserIdentity(String openId,
                                    String unionId,
                                    String nickname,
                                    String avatarUrl) {
}
