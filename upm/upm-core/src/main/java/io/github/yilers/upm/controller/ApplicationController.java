package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.github.yilers.api.base.BaseOperateRequest;
import io.github.yilers.api.validated.Add;
import io.github.yilers.api.validated.Update;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.ApplicationHandler;
import io.github.yilers.upm.request.ApplicationRequest;
import io.github.yilers.upm.response.ApplicationResponse;
import io.github.yilers.upm.response.ApplicationSecretResponse;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/application")
@RequiredArgsConstructor
@Tag(name = "应用管理")
public class ApplicationController {
    private final ApplicationHandler applicationHandler;

    @GetMapping("/findAll")
    @Operation(summary = "查询当前租户应用")
    @SaCheckPermission(value = {"system:application:list", "system:menu:list", "system:role:permission"}, mode = SaMode.OR)
    public Result<List<ApplicationResponse>> findAll() {
        return Result.ok(applicationHandler.findAll());
    }

    @PostMapping("/save")
    @Operation(summary = "新增应用")
    @SysLog(module = "应用模块", value = "新增应用")
    @SaCheckPermission("system:application:add")
    public Result<?> save(@Validated(Add.class) @RequestBody ApplicationRequest dto) {
        applicationHandler.save(dto);
        return Result.ok();
    }

    @PostMapping("/update")
    @Operation(summary = "修改应用")
    @SysLog(module = "应用模块", value = "修改应用")
    @SaCheckPermission("system:application:edit")
    public Result<?> update(@Validated(Update.class) @RequestBody ApplicationRequest dto) {
        applicationHandler.update(dto);
        return Result.ok();
    }

    @PostMapping("/usable")
    @Operation(summary = "启停应用")
    @SysLog(module = "应用模块", value = "启停应用")
    @SaCheckPermission("system:application:usable")
    public Result<?> usable(@Validated @RequestBody BaseOperateRequest dto) {
        applicationHandler.usable(dto.getId());
        return Result.ok();
    }

    @PostMapping("/resetSecret")
    @Operation(summary = "生成或重置SSO客户端密钥",
            description = "密钥明文仅在本次响应中返回。重置后旧密钥立即失效，应立即更新业务系统配置。")
    @SysLog(module = "应用模块", value = "重置SSO客户端密钥")
    @SaCheckPermission("system:application:edit")
    public Result<ApplicationSecretResponse> resetSecret(@Validated @RequestBody BaseOperateRequest dto) {
        return Result.ok(applicationHandler.resetSecret(dto.getId()));
    }
}
