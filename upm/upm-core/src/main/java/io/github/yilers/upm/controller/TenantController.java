package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.entity.Tenant;
import io.github.yilers.upm.request.TenantRequest;
import io.github.yilers.upm.service.TenantService;
import io.github.yilers.web.log.SysLog;
import io.github.yilers.web.validated.Add;
import io.github.yilers.web.validated.Update;
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
public class TenantController {
    private final TenantService tenantService;

    @Operation(summary = "查询所有租户")
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
        tenantService.addTenant(dto);
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


}
