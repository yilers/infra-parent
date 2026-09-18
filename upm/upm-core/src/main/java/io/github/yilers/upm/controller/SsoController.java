package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.sso.processor.SaSsoServerProcessor;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.SsoHandler;
import io.github.yilers.upm.request.SsoAuthorizeRequest;
import io.github.yilers.upm.response.SsoApplicationResponse;
import io.github.yilers.upm.response.SsoAuthorizeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sso")
@RequiredArgsConstructor
@Tag(name = "单点登录")
public class SsoController {
    private final SsoHandler ssoHandler;

    @SaIgnore
    @GetMapping("/application")
    @Operation(summary = "查询SSO应用公开信息",
            description = "授权页面展示租户和应用名称、Logo等公开信息，不需要登录且不会返回客户端密钥。")
    public Result<SsoApplicationResponse> application(
            @Parameter(description = "SSO客户端标识，格式为“租户编码:应用编码”", example = "yilers.com:oa")
            @RequestParam String client) {
        return Result.ok(ssoHandler.findApplication(client));
    }

    @PostMapping("/authorize")
    @Operation(summary = "创建一次性SSO授权ticket",
            description = "需要携带UPM登录token。校验用户、租户、应用权限和回调白名单后，返回携带短时一次性ticket的回跳地址。")
    public Result<SsoAuthorizeResponse> authorize(@Valid @RequestBody SsoAuthorizeRequest request) {
        return Result.ok(ssoHandler.authorize(request));
    }

    /**
     * Sa-Token 模式三消息入口：业务后端通过签名请求校验 ticket，也通过此入口参与单点注销。
     */
    @SaIgnore
    @RequestMapping("/pushS")
    @Operation(summary = "接收SSO Client签名消息",
            description = "Sa-Token SSO模式三原生服务端入口。业务后端使用client和客户端密钥签名调用，完成ticket校验、登录上下文获取及单点注销登记。")
    public Object pushServerMessage() {
        return SaSsoServerProcessor.instance.ssoPushS();
    }
}
