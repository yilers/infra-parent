package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.UserBatchHandler;
import io.github.yilers.upm.handler.UserHandler;
import io.github.yilers.upm.request.UserBatchAddRequest;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.request.UserProfileUpdateRequest;
import io.github.yilers.upm.request.UserRequest;
import io.github.yilers.upm.request.UserUpdatePwdRequest;
import io.github.yilers.upm.response.UserBatchAddResponse;
import io.github.yilers.upm.response.UserInfoResponse;
import io.github.yilers.api.base.BaseOperateRequest;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.web.log.SysLog;
import io.github.yilers.web.permission.DataPermissionResource;
import io.github.yilers.api.validated.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户")
@ApiSupport(order = 100, author = "yilers")
public class UserController {
    private final UserHandler userHandler;
    private final UserBatchHandler userBatchHandler;

    @GetMapping("/current")
    @Operation(summary = "当前用户信息")
    public Result<UserInfoResponse> currentInfo() {
        UserInfoResponse userInfo = userHandler.currentInfo();
        return Result.ok(userInfo);
    }

    @PostMapping("/profile/update")
    @Operation(summary = "修改当前用户个人资料")
    @SysLog(module = "用户模块", value = "修改个人资料")
    public Result<?> updateProfile(@Validated @RequestBody UserProfileUpdateRequest request) {
        userHandler.updateProfile(request);
        return Result.ok();
    }

    @PostMapping("/addUser")
    @Operation(summary = "添加用户")
    @SysLog(module = "用户模块", value = "添加用户")
    @SaCheckPermission("system:user:add")
    public Result<Long> addUser(@Validated @RequestBody UserRequest request) {
        return Result.ok(userHandler.addUser(request));
    }

    @PostMapping("/batch/add")
    @Operation(summary = "批量添加用户")
    @SysLog(module = "用户模块", value = "批量添加用户", recordParams = false)
    @DataPermissionResource(name = "批量新增用户")
    @SaCheckPermission("system:user:batch:add")
    public Result<UserBatchAddResponse> batchAdd(@Validated @RequestBody UserBatchAddRequest request) {
        return Result.ok(userBatchHandler.add(request));
    }

    @PostMapping("/updateUser")
    @Operation(summary = "更新用户")
    @SysLog(module = "用户模块", value = "更新用户", hideFieldList = {"password", "idCard"})
    @SaCheckPermission("system:user:edit")
    public Result<?> updateUser(@Validated(Update.class) @RequestBody UserRequest request) {
        userHandler.updateUser(request);
        return Result.ok();
    }

    @PostMapping("/usable")
    @Operation(summary = "切换用户可用状态")
    @SysLog(module = "用户模块", value = "切换可用状态")
    @SaCheckPermission("system:user:edit")
    public Result<?> usable(@Validated @RequestBody BaseOperateRequest dto) {
        userHandler.usable(dto);
        return Result.ok();
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询用户")
    @DataPermissionResource(name = "用户列表")
    @SaCheckPermission("system:user:list")
    public Result<Page<UserInfoResponse>> page(@RequestBody BasePageRequest<UserPageRequest> request) {
        Page<UserInfoResponse> p = userHandler.page(request);
        return Result.ok(p);
    }

    @PostMapping("/delete/{userId}")
    @Operation(summary = "删除用户")
    @SysLog(module = "用户模块", value = "删除用户")
    @SaCheckPermission("system:user:delete")
    public Result<?> deleteById(@PathVariable("userId") @NotNull Long userId) {
        userHandler.deleteById(userId);
        return Result.ok();
    }

    @PostMapping("/login-lock/unlock/{userId}")
    @Operation(summary = "解除用户登录锁定")
    @SysLog(module = "用户模块", value = "解除登录锁定")
    @SaCheckPermission("system:user:unlock")
    public Result<?> unlockLogin(@PathVariable("userId") @NotNull Long userId) {
        userHandler.unlockLogin(userId);
        return Result.ok();
    }

    @PostMapping("/updatePwd")
    @Operation(summary = "修改密码")
    @SysLog(module = "用户模块", value = "修改密码")
    public Result<?> updatePwd(@Validated @RequestBody UserUpdatePwdRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        request.setUserId(userId);
        userHandler.updatePwd(request);
        return Result.ok();
    }

}
