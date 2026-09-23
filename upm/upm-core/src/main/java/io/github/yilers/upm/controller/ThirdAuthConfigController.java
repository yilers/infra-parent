package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.api.base.BaseOperateRequest;
import io.github.yilers.api.validated.Add;
import io.github.yilers.api.validated.Update;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.ThirdAuthConfigHandler;
import io.github.yilers.upm.request.ThirdAuthConfigRequest;
import io.github.yilers.upm.response.ThirdAuthConfigResponse;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 第三方认证平台配置管理接口。
 */
@RestController
@RequestMapping("/thirdAuthConfig")
@RequiredArgsConstructor
@Tag(name = "第三方认证配置")
public class ThirdAuthConfigController {

    private final ThirdAuthConfigHandler thirdAuthConfigHandler;

    @GetMapping("/findAll")
    @Operation(summary = "查询当前租户第三方认证配置",
            description = "Client Secret只返回是否已配置，不返回数据库中保存的原文。")
    @SaCheckPermission("system:thirdAuth:list")
    public Result<List<ThirdAuthConfigResponse>> findAll() {
        return Result.ok(thirdAuthConfigHandler.findAll());
    }

    @PostMapping("/save")
    @Operation(summary = "新增第三方认证配置", description = "新增配置默认停用，需要确认参数完整后再启用。")
    @SysLog(module = "第三方认证配置模块", value = "新增第三方认证配置", hideFieldList = "clientSecret")
    @SaCheckPermission("system:thirdAuth:add")
    public Result<?> save(@Validated(Add.class) @RequestBody ThirdAuthConfigRequest request) {
        thirdAuthConfigHandler.save(request);
        return Result.ok();
    }

    @PostMapping("/update")
    @Operation(summary = "修改第三方认证配置",
            description = "平台编码不可修改；Client Secret留空时保留数据库原值。")
    @SysLog(module = "第三方认证配置模块", value = "修改第三方认证配置", hideFieldList = "clientSecret")
    @SaCheckPermission("system:thirdAuth:edit")
    public Result<?> update(@Validated(Update.class) @RequestBody ThirdAuthConfigRequest request) {
        thirdAuthConfigHandler.update(request);
        return Result.ok();
    }

    @PostMapping("/usable")
    @Operation(summary = "启停第三方认证配置", description = "启用前会校验Client ID、Client Secret和回调地址。")
    @SysLog(module = "第三方认证配置模块", value = "启停第三方认证配置")
    @SaCheckPermission("system:thirdAuth:usable")
    public Result<?> usable(@Validated @RequestBody BaseOperateRequest request) {
        thirdAuthConfigHandler.usable(request.getId());
        return Result.ok();
    }
}
