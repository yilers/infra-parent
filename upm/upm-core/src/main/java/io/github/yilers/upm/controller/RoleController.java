package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.RoleHandler;
import io.github.yilers.upm.request.RolePermissionRequest;
import io.github.yilers.upm.request.RoleRequest;
import io.github.yilers.upm.request.RoleUserRequest;
import io.github.yilers.upm.response.RoleInfoResponse;
import io.github.yilers.web.base.BaseOperateRequest;
import io.github.yilers.web.base.BasePageRequest;
import io.github.yilers.web.log.SysLog;
import io.github.yilers.web.validated.Add;
import io.github.yilers.web.validated.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@Tag(name = "角色")
public class RoleController {
    private final RoleHandler roleHandler;

    @PostMapping("/save")
    @Operation(summary = "新增角色")
    @ApiOperationSupport(order = 1)
    @SysLog(module = "角色模块", value = "新增角色")
    @SaCheckPermission("system:role:add")
    public Result<?> save(@Validated(Add.class) @RequestBody RoleRequest roleRequest) {
        roleHandler.save(roleRequest);
        return Result.ok();
    }

    @PostMapping("/updateById")
    @Operation(summary = "修改角色")
    @ApiOperationSupport(order = 3)
    @SysLog(module = "角色模块", value = "修改角色")
    @SaCheckPermission("system:role:edit")
    public Result<?> updateById(@Validated(Update.class) @RequestBody RoleRequest roleRequest) {
        roleHandler.updateById(roleRequest);
        return Result.ok();
    }

    @PostMapping("/delete/{roleId}")
    @Operation(summary = "删除角色")
    @ApiOperationSupport(order = 4)
    @SysLog(module = "角色模块", value = "删除角色")
    @SaCheckPermission("system:role:delete")
    public Result<?> deleteById(@PathVariable("roleId") @NotNull Long roleId) {
        roleHandler.deleteById(roleId);
        return Result.ok();
    }

    @PostMapping("/bindPermission")
    @Operation(summary = "角色资源绑定")
    @ApiOperationSupport(order = 5)
    @SysLog(module = "角色模块", value = "角色资源绑定")
    @SaCheckPermission("system:role:edit")
    public Result<?> bindPermission(@Validated @RequestBody RolePermissionRequest dto) {
        roleHandler.bindPermission(dto);
        return Result.ok();
    }


    @PostMapping("/usable")
    @Operation(summary = "切换角色可用状态")
    @ApiOperationSupport(order = 5)
    @SysLog(module = "角色模块", value = "切换角色可用状态")
    @SaCheckPermission("system:role:edit")
    public Result<?> usable(@Validated @RequestBody BaseOperateRequest dto) {
        roleHandler.usable(dto);
        return Result.ok();
    }

    @GetMapping("/findById/{roleId}")
    @Operation(summary = "通过id查询角色信息")
    @ApiOperationSupport(order = 7)
    public Result<RoleInfoResponse> findById(@PathVariable("roleId") @Parameter(description = "角色id") @NotNull Long roleId) {
        RoleInfoResponse roleInfoResponse = roleHandler.findById(roleId);
        return Result.ok(roleInfoResponse);
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询角色")
//    @SaCheckPermission("system:role:list")
    public Result<Page<RoleInfoResponse>> page(@RequestBody BasePageRequest<RoleRequest> request) {
        Page<RoleInfoResponse> p = roleHandler.page(request);
        return Result.ok(p);
    }

    @PostMapping("/bindUser")
    @Operation(summary = "角色绑定用户")
    @ApiOperationSupport(order = 5)
    @SysLog(module = "角色模块", value = "角色绑定用户")
    @SaCheckPermission("system:role:bindUser")
    public Result<?> bindUser(@Validated @RequestBody RoleUserRequest request) {
        roleHandler.bindUser(request);
        return Result.ok();
    }

}
