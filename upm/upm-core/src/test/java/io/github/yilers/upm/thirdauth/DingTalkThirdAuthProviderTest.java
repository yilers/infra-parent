package io.github.yilers.upm.thirdauth;

import io.github.yilers.upm.entity.ThirdAuthConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DingTalkThirdAuthProviderTest {

    @Test
    void buildsEncodedAuthorizeUrlWithRequiredOpenIdScope() {
        ThirdAuthConfig config = new ThirdAuthConfig();
        config.setClientId("ding-test-client");
        config.setRedirectUri("https://upm.example.com/third-auth-callback.html");
        config.setScopes("corpid");
        DingTalkThirdAuthProvider provider = new DingTalkThirdAuthProvider(RestClient.create());

        URI uri = URI.create(provider.buildAuthorizeUrl(config, "test-state"));
        String query = URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8);

        assertEquals("login.dingtalk.com", uri.getHost());
        assertTrue(query.contains("client_id=ding-test-client"));
        assertTrue(query.contains("redirect_uri=https://upm.example.com/third-auth-callback.html"));
        assertTrue(query.contains("scope=openid corpid"));
        assertTrue(query.contains("state=test-state"));
        assertTrue(query.contains("prompt=consent"));
    }
}
