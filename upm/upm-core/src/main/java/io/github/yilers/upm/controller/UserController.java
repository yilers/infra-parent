package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.UserHandler;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.request.UserRequest;
import io.github.yilers.upm.request.UserUpdatePwdRequest;
import io.github.yilers.upm.response.UserInfoResponse;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.base.BaseOperateRequest;
import io.github.yilers.web.base.BasePageRequest;
import io.github.yilers.web.log.SysLog;
import io.github.yilers.web.validated.Update;
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
public class UserController {
    private final UserService userService;
    private final UserHandler userHandler;

    @GetMapping("/current")
    @Operation(summary = "当前用户信息")
    public Result<UserInfoResponse> currentInfo() {
        UserInfoResponse userInfo = userHandler.currentInfo();
        return Result.ok(userInfo);
    }

    @PostMapping("/addUser")
    @Operation(summary = "添加用户")
    @SysLog(module = "用户模块", value = "添加用户")
    @SaCheckPermission("system:user:add")
    public Result<Long> addUser(@Validated @RequestBody UserRequest request) {
        return Result.ok(userHandler.addUser(request));
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
