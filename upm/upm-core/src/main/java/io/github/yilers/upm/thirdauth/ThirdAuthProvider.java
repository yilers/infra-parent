package io.github.yilers.upm.thirdauth;

import io.github.yilers.upm.entity.ThirdAuthConfig;

/**
 * 第三方认证平台适配器。
 *
 * <p>平台HTTP协议封装在Provider内部，账号绑定业务不直接依赖钉钉等平台的请求响应结构。</p>
 */
public interface ThirdAuthProvider {

    /** 平台编码。 */
    String platform();

    /** 构造用户授权地址。 */
    String buildAuthorizeUrl(ThirdAuthConfig config, String state);

    /** 使用一次性授权码换取用户身份。 */
    ThirdAuthUserIdentity exchangeUserIdentity(ThirdAuthConfig config, String authCode);
}
