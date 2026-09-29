package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.handler.CommonHandler;
import io.github.yilers.upm.handler.TenantHandler;
import io.github.yilers.upm.handler.TenantLoginPolicyHandler;
import io.github.yilers.upm.handler.TenantPermissionSyncHandler;
import io.github.yilers.upm.entity.TenantLoginPolicy;
import io.github.yilers.upm.request.TenantLoginPolicyRequest;
import io.github.yilers.upm.request.TenantRequest;
import io.github.yilers.upm.response.TenantPermissionSyncResponse;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.web.log.SysLog;
import io.github.yilers.api.validated.Add;
import io.github.yilers.api.validated.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
@Tag(name = "租户")
@ApiSupport(order = 40, author = "yilers")
public class TenantController {
    private final TenantService tenantService;
    private final CommonHandler commonHandler;
    private final TenantHandler tenantHandler;
    private final TenantPermissionSyncHandler tenantPermissionSyncHandler;
    private final TenantLoginPolicyHandler tenantLoginPolicyHandler;

    @Operation(summary = "查询所有租户(没有删除的)")
    @GetMapping("/findAll")
    @SaCheckPermission("system:tenant:list")
    public Result<List<Tenant>> findAll() {
        return Result.ok(tenantService.list());
    }

    @Operation(summary = "保存租户")
    @PostMapping("/save")
    @SysLog(module = "租户模块", value = "保存租户")
    @SaCheckPermission("system:tenant:add")
    public Result<?> save(@Validated(Add.class) @RequestBody TenantRequest dto) {
        commonHandler.addTenant(dto);
        return Result.ok();
    }

    @Operation(summary = "根据id更新租户")
    @PostMapping("/updateById")
    @SysLog(module = "租户模块", value = "更新租户")
    @SaCheckPermission("system:tenant:edit")
    public Result<?> updateById(@Validated(Update.class) @RequestBody TenantRequest dto) {
        tenantService.updateTenant(dto);
        return Result.ok();
    }

    @Operation(summary = "通过id查询租户")
    @GetMapping("/findById/{id}")
    public Result<Tenant> findById(@PathVariable Serializable id) {
        return Result.ok(tenantService.getById(id));
    }

    @Operation(summary = "删除租户")
    @PostMapping("/deleteById/{id}")
    @SysLog(module = "租户模块", value = "删除租户")
    @SaCheckPermission("system:tenant:delete")
    public Result<?> deleteById(@PathVariable Long id) {
        tenantHandler.deleteById(id);
        return Result.ok();
    }

    @Operation(summary = "预览租户应用菜单同步差异")
    @GetMapping("/{id}/permission-sync/preview")
    @SaCheckPermission("system:tenant:sync")
    public Result<TenantPermissionSyncResponse> previewPermissionSync(@PathVariable Long id) {
        return Result.ok(tenantPermissionSyncHandler.preview(id));
    }

    @Operation(summary = "同步租户应用菜单")
    @PostMapping("/{id}/permission-sync")
    @SysLog(module = "租户模块", value = "同步租户应用菜单")
    @SaCheckPermission("system:tenant:sync")
    public Result<TenantPermissionSyncResponse> syncPermission(@PathVariable Long id) {
        return Result.ok(tenantPermissionSyncHandler.sync(id));
    }

    @Operation(summary = "查询租户账号密码登录策略",
            description = "未配置时data为null，表示该租户不限制密码失败次数。")
    @GetMapping("/login-policy/find/{id}")
    @SaCheckPermission("system:tenant:edit")
    public Result<TenantLoginPolicy> findLoginPolicy(@PathVariable Long id) {
        return Result.ok(tenantLoginPolicyHandler.find(id));
    }

    @Operation(summary = "保存租户账号密码登录策略",
            description = "策略不存在时新增，已经存在时按version乐观锁修改。")
    @PostMapping("/login-policy/save/{id}")
    @SysLog(module = "租户模块", value = "保存租户登录策略")
    @SaCheckPermission("system:tenant:edit")
    public Result<?> saveLoginPolicy(@PathVariable Long id,
                                     @Validated @RequestBody TenantLoginPolicyRequest request) {
        tenantLoginPolicyHandler.save(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除租户账号密码登录策略",
            description = "物理删除策略；删除后该租户不限制密码失败次数。")
    @PostMapping("/login-policy/delete/{id}")
    @SysLog(module = "租户模块", value = "删除租户登录策略")
    @SaCheckPermission("system:tenant:edit")
    public Result<?> deleteLoginPolicy(@PathVariable Long id) {
        tenantLoginPolicyHandler.delete(id);
        return Result.ok();
    }


}
