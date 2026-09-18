package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SSO授权结果")
public record SsoAuthorizeResponse(
        @Schema(description = "携带一次性ticket和state的业务应用回跳地址",
                example = "https://oa.example.com/sso/callback?ticket=xxxx&state=xxxx") String redirectUrl) {
}
