package com.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.github.yilers.core.constant.CommonConst;
import com.github.yilers.core.util.Result;
import com.github.yilers.upm.entity.Permission;
import com.github.yilers.upm.handler.PermissionHandler;
import com.github.yilers.upm.request.PermissionRequest;
import com.github.yilers.upm.request.SortMoveRequest;
import com.github.yilers.web.log.SysLog;
import com.github.yilers.web.validated.Add;
import com.github.yilers.web.validated.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
@Tag(name = "权限")
public class PermissionController {
    private final PermissionHandler permissionHandler;

    @PostMapping("/save")
    @Operation(summary = "新增权限资源")
    @ApiOperationSupport(order = 1)
    @SysLog(module = "权限资源模块", value = "新增权限资源")
    @SaCheckPermission("system:menu:add")
    public Result<?> save(@Validated(Add.class) @RequestBody PermissionRequest dto) {
        permissionHandler.save(dto);
        return Result.ok();
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除权限资源")
    @ApiOperationSupport(order = 2)
    @SysLog(module = "权限资源模块", value = "删除权限资源")
    @SaCheckPermission("system:menu:delete")
    public Result<?> deleteById(@PathVariable("id") Long id) {
        permissionHandler.deleteById(id);
        return Result.ok();
    }


    @PostMapping("/update")
    @Operation(summary = "修改权限资源")
    @ApiOperationSupport(order = 3)
    @SysLog(module = "权限资源模块", value = "修改权限资源")
    @SaCheckPermission("system:menu:edit")
    public Result<?> update(@Validated(Update.class) @RequestBody PermissionRequest dto) {
        permissionHandler.update(dto);
        return Result.ok();
    }

    @PostMapping("/sortOrder")
    @Operation(summary = "权限移动排序")
    @ApiOperationSupport(order = 5)
    @SaCheckPermission("system:menu:edit")
    public Result<?> sortOrder(@RequestBody SortMoveRequest dto) {
        permissionHandler.sortOrder(dto);
        return Result.ok();
    }

    @GetMapping("/current")
    @Operation(summary = "获取当前人的权限列表")
    @ApiOperationSupport(order = 6)
    public Result<List<Permission>> currentInfo(String device) {
        List<Permission> permissionList = permissionHandler.currentInfo(device);
        return Result.ok(permissionList);
    }

    @GetMapping("/findAll")
    @Operation(summary = "平台管理员获取所有权限列表")
    @ApiOperationSupport(order = 7)
    @SaCheckRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)
    public Result<List<Permission>> findAll(String device) {
        List<Permission> permissionList = permissionHandler.findAll(device);
        return Result.ok(permissionList);
    }

}
