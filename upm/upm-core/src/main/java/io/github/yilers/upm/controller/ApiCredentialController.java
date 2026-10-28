package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.credential.ApiCredentialHandler;
import io.github.yilers.upm.credential.ApiPermissionCatalog;
import io.github.yilers.upm.entity.ApiCredential;
import io.github.yilers.upm.request.ApiCredentialRequest;
import io.github.yilers.upm.request.ApiCredentialSecretResetRequest;
import io.github.yilers.upm.response.ApiCredentialSecretResponse;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 管理端使用用户登录态；不允许机器凭证管理其他凭证。 */
@RestController
@RequestMapping("/api-credential")
@RequiredArgsConstructor
@Tag(name = "系统调用凭证")
public class ApiCredentialController {
    private final ApiCredentialHandler handler;
    private final ApiPermissionCatalog catalog;

    @GetMapping("/list")
    @SaCheckPermission("system:api-credential:list")
    @Operation(summary = "查询当前租户调用凭证，不返回密钥")
    public Result<List<ApiCredential>> list() { return Result.ok(handler.list()); }

    @GetMapping("/permissions/{id}")
    @SaCheckPermission("system:api-credential:list")
    @Operation(summary = "查询凭证已授予的接口权限")
    public Result<List<String>> permissions(@PathVariable Long id) { return Result.ok(handler.permissions(id)); }

    @GetMapping("/catalog")
    @SaCheckPermission("system:api-credential:list")
    @Operation(summary = "查询当前进程可授权的系统调用能力")
    public Result<List<ApiPermissionCatalog.Capability>> catalog() { return Result.ok(catalog.list()); }

    @PostMapping("/save")
    @SysLog(module = "系统调用凭证", value = "创建凭证", recordParams = false)
    @SaCheckPermission("system:api-credential:add")
    @Operation(summary = "创建调用凭证，密钥仅返回一次")
    public Result<ApiCredentialSecretResponse> create(@Valid @RequestBody ApiCredentialRequest request,
                                                      jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.ok(handler.create(request));
    }

    @PostMapping("/update")
    @SysLog(module = "系统调用凭证", value = "修改凭证和授权", recordParams = false)
    @SaCheckPermission("system:api-credential:edit")
    @Operation(summary = "修改凭证状态、期限和接口授权")
    public Result<Void> update(@Valid @RequestBody ApiCredentialRequest request) {
        handler.update(request);
        return Result.ok();
    }

    @PostMapping("/resetSecret")
    @SysLog(module = "系统调用凭证", value = "重置凭证密钥", recordParams = false)
    @SaCheckPermission("system:api-credential:edit")
    @Operation(summary = "重置调用密钥",
            description = "保留原Key和授权，新Secret仅在本次返回；提交后旧Secret不再用于新的验签请求。")
    public Result<ApiCredentialSecretResponse> resetSecret(@Valid @RequestBody ApiCredentialSecretResetRequest request,
                                                           jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.ok(handler.resetSecret(request.getId(), request.getVersion()));
    }
}
