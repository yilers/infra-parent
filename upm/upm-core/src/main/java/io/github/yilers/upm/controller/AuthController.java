package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.AuthHandler;
import io.github.yilers.upm.request.LoginRequest;
import io.github.yilers.upm.response.LoginResponse;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证")
public class AuthController {
    private final AuthHandler authHandler;

    @SaIgnore
    @PostMapping("/admin")
    @Operation(summary = "登录")
    @SysLog(module = "认证模块", value = "登录", hideFieldList = "password")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest loginRequest) {
        LoginResponse login = authHandler.login(loginRequest);
        return Result.ok(login);
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    @SysLog(module = "认证模块", value = "登出")
    public Result<LoginResponse> logout() {
        try {
            long userId = StpUtil.getLoginIdAsLong();
            authHandler.logout(userId);
        } catch (Exception e) {
            log.error("退出出错", e);
        }
        return Result.ok();
    }
}
