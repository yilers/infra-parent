package com.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yilers.core.util.Result;
import com.github.yilers.upm.entity.Log;
import com.github.yilers.upm.response.LogInfoResponse;
import com.github.yilers.upm.service.LogService;
import com.github.yilers.web.base.BasePageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/log")
@RequiredArgsConstructor
@Tag(name = "日志")
public class LogController {
    private final LogService logService;

    @PostMapping("/page")
    @Operation(summary = "日志分页")
    @SaCheckPermission("system:log:list")
    public Result<Page<LogInfoResponse>> page(@RequestBody BasePageRequest<Log> request) {
        Page<LogInfoResponse> p = logService.findByPage(request);
        return Result.ok(p);
    }

}
