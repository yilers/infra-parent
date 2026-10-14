package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.DeptLeaderHandler;
import io.github.yilers.upm.request.DeptLeaderSaveRequest;
import io.github.yilers.upm.response.DeptLeaderCandidateResponse;
import io.github.yilers.upm.response.DeptLeaderResponse;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门主管设置接口，与部门基本资料编辑权限分离。
 */
@RestController
@RequestMapping("/dept/leader")
@RequiredArgsConstructor
@Tag(name = "部门主管")
public class DeptLeaderController {

    private final DeptLeaderHandler deptLeaderHandler;

    @GetMapping("/visible")
    @SaCheckPermission("system:dept:list")
    @Operation(summary = "查询当前用户可见部门的主管信息")
    public Result<List<DeptLeaderResponse>> visible() {
        return Result.ok(deptLeaderHandler.findVisible());
    }

    @GetMapping("/current/{deptId}")
    @SaCheckPermission("system:dept:list")
    @Operation(summary = "查询部门当前主管和副主管")
    public Result<List<DeptLeaderResponse>> current(@PathVariable Long deptId) {
        return Result.ok(deptLeaderHandler.findCurrentByDeptId(deptId));
    }

    @GetMapping("/candidates/{deptId}")
    @SaCheckPermission("system:dept:leader:edit")
    @Operation(summary = "搜索部门主管候选员工")
    public Result<List<DeptLeaderCandidateResponse>> candidates(
            @PathVariable Long deptId,
            @Parameter(description = "按姓名搜索") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否搜索本租户所有员工，默认仅当前部门及下级")
            @RequestParam(defaultValue = "false") boolean includeAll) {
        return Result.ok(deptLeaderHandler.findCandidates(deptId, keyword, includeAll));
    }

    @PostMapping("/save")
    @SaCheckPermission("system:dept:leader:edit")
    @SysLog(module = "机构模块", value = "设置部门主管")
    @Operation(summary = "设置部门当前主管和副主管")
    public Result<?> save(@Valid @RequestBody DeptLeaderSaveRequest request) {
        deptLeaderHandler.save(request);
        return Result.ok();
    }
}
