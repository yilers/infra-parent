package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.api.field.FieldAccess;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.FieldPermissionHandler;
import io.github.yilers.upm.request.RoleFieldRequest;
import io.github.yilers.upm.response.RoleFieldResponse;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 字段目录及角色字段授权入口，数据范围和可授权上限由Handler再次校验。 */
@RestController
@RequestMapping("/field")
@RequiredArgsConstructor
@Tag(name = "字段权限")
public class FieldPermissionController {
    private final FieldPermissionHandler handler;

    @GetMapping("/current")
    @Operation(summary = "当前用户的字段策略", description = "仅返回当前可信登录人的策略，未出现的受保护字段默认隐藏。")
    public Result<Map<String, FieldAccess>> current() {
        return Result.ok(handler.currentAccess());
    }

    @PostMapping("/synchronize")
    @SaCheckPermission("system:role:field")
    @Operation(summary = "同步当前租户代码声明的字段目录")
    @SysLog(module = "字段权限", value = "同步字段目录")
    public Result<?> synchronize() {
        handler.synchronize();
        return Result.ok();
    }

    @GetMapping("/role/{roleId}")
    @SaCheckPermission("system:role:field")
    @Operation(summary = "查询角色字段权限及授权上限")
    public Result<RoleFieldResponse> role(@PathVariable Long roleId) {
        return Result.ok(handler.findByRole(roleId));
    }

    @PostMapping("/role/save")
    @SaCheckPermission("system:role:field")
    @Operation(summary = "保存角色字段权限", description = "校验租户、角色版本及当前人授权上限，未提交字段保持不变。")
    @SysLog(module = "字段权限", value = "保存角色字段权限")
    public Result<?> save(@Valid @RequestBody RoleFieldRequest request) {
        handler.save(request);
        return Result.ok();
    }
}
