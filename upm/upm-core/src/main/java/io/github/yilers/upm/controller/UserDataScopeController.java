package io.github.yilers.upm.controller;

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
public class UserDataScopeController {
    private final UserDataScopeService userDataScopeService;

    @GetMapping("/findByUserId/{userId}")
    @Operation(summary = "通过用户id获取已经绑定的")
    public Result<List<UserDataScope>> findByUserId(@PathVariable("userId") Long userId) {
        List<UserDataScope> list = userDataScopeService.findByUserId(userId);
        return Result.ok(list);
    }

    @PostMapping("/bind")
    @Operation(summary = "绑定数据权限")
    @SysLog(module = "用户模块", value = "用户绑定数据权限")
    public Result<?> bind(@Validated @RequestBody UserDataScopeRequest request) {
        userDataScopeService.bind(request);
        return Result.ok();
    }
}
