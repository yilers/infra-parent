package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.entity.UserDataScope;
import io.github.yilers.upm.request.UserDataScopeRequest;
import io.github.yilers.upm.service.UserDataScopeService;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/userDataScope")
@RequiredArgsConstructor
@Tag(name = "用户数据权限")
@ApiSupport(order = 110, author = "yilers")
public class UserDataScopeController {
    private final UserDataScopeService userDataScopeService;
    private final UserService userService;

    @GetMapping("/findByUserId/{userId}")
    @SaCheckPermission("system:user:dataScope")
    @Operation(summary = "通过用户id获取已经绑定的")
    public Result<List<UserDataScope>> findByUserId(@PathVariable("userId") Long userId) {
        if (userService.getById(userId) == null) {
            throw new CommonException("用户不存在或不属于当前租户");
        }
        List<UserDataScope> list = userDataScopeService.findByUserId(userId);
        return Result.ok(list);
    }

    @PostMapping("/bind")
    @SaCheckPermission("system:user:dataScope")
    @Operation(summary = "绑定数据权限")
    @SysLog(module = "用户模块", value = "用户绑定数据权限")
    public Result<?> bind(@Validated @RequestBody UserDataScopeRequest request) {
        userDataScopeService.bind(request);
        return Result.ok();
    }
}
