package io.github.yilers.upm.thirdauth;

import cn.hutool.v7.core.text.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.yilers.upm.entity.ThirdAuthConfig;
import io.github.yilers.upm.enums.ThirdAuthPlatformEnum;
import io.github.yilers.web.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static io.github.yilers.upm.config.ThirdAuthHttpConfig.THIRD_AUTH_REST_CLIENT;

/**
 * 钉钉OAuth2用户授权适配器。
 */
@Slf4j
@Component
public class DingTalkThirdAuthProvider implements ThirdAuthProvider {

    private static final String AUTHORIZE_URL = "https://login.dingtalk.com/oauth2/auth";
    private static final String USER_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";
    private static final String CURRENT_USER_URL = "https://api.dingtalk.com/v1.0/contact/users/me";

    private final RestClient restClient;

    public DingTalkThirdAuthProvider(@Qualifier(THIRD_AUTH_REST_CLIENT) RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String platform() {
        return ThirdAuthPlatformEnum.DING_TALK.getCode();
    }

    @Override
    public String buildAuthorizeUrl(ThirdAuthConfig config, String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", config.getClientId())
                .queryParam("scope", resolveScope(config.getScopes()))
                .queryParam("state", state)
                .queryParam("prompt", "consent")
                .build().encode().toUriString();
    }

    @Override
    public ThirdAuthUserIdentity exchangeUserIdentity(ThirdAuthConfig config, String authCode) {
        try {
            DingTalkUserToken token = restClient.post()
                    .uri(USER_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "clientId", config.getClientId(),
                            "clientSecret", config.getClientSecret(),
                            "code", authCode,
                            "grantType", "authorization_code"))
                    .retrieve()
                    .body(DingTalkUserToken.class);
            if (token == null || StrUtil.isBlank(token.accessToken())) {
                throw new CommonException("钉钉未返回用户访问凭证");
            }
            DingTalkUserProfile profile = restClient.get()
                    .uri(CURRENT_USER_URL)
                    .header("x-acs-dingtalk-access-token", token.accessToken())
                    .retrieve()
                    .body(DingTalkUserProfile.class);
            if (profile == null || StrUtil.isBlank(profile.openId())) {
                throw new CommonException("钉钉未返回用户身份信息");
            }
            return new ThirdAuthUserIdentity(profile.openId(), profile.unionId(), profile.nick(),
                    profile.avatarUrl());
        } catch (CommonException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("钉钉用户授权失败: {}", exception.getClass().getSimpleName());
            throw new CommonException("钉钉授权失败，请重新发起绑定");
        }
    }

    private String resolveScope(String configuredScopes) {
        LinkedHashSet<String> scopes = new LinkedHashSet<>();
        scopes.add("openid");
        if (StrUtil.isNotBlank(configuredScopes)) {
            scopes.addAll(List.of(configuredScopes.split(",")));
        }
        return String.join(" ", scopes);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DingTalkUserToken(String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DingTalkUserProfile(String openId,
                                       String unionId,
                                       String nick,
                                       String avatarUrl) {
    }
}
