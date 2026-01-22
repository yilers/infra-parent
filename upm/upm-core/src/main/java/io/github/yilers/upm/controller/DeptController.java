package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.github.yilers.core.util.Result;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.handler.DeptHandler;
import io.github.yilers.upm.request.DeptRequest;
import io.github.yilers.upm.request.SortMoveRequest;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.web.log.SysLog;
import io.github.yilers.web.validated.Add;
import io.github.yilers.web.validated.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/dept")
@RequiredArgsConstructor
@Tag(name = "部门")
public class DeptController {
    private final DeptService deptService;
    private final DeptHandler deptHandler;


    @ApiOperationSupport(order = 1)
    @Operation(summary = "机构添加")
    @PostMapping(value = "/save")
    @SysLog(module = "机构模块", value = "新增机构")
    @SaCheckPermission("system:dept:add")
    public Result<?> save(@Validated(Add.class) @RequestBody DeptRequest dto) {
        deptHandler.save(dto);
        return Result.ok();
    }


    @ApiOperationSupport(order = 2)
    @Operation(summary = "当前人部门树数据")
    @PostMapping(value = "/current")
    public Result<List<Dept>> current() {
        List<Dept> list = deptHandler.current();
        return Result.ok(list);
    }

    @ApiOperationSupport(order = 3)
    @Operation(summary = "机构更新")
    @PostMapping(value = "/updateById")
    @SysLog(module = "机构模块", value = "机构更新")
    @SaCheckPermission("system:dept:edit")
    public Result<?> updateById(@Validated(Update.class) @RequestBody DeptRequest deptDTO) {
        deptHandler.updateById(deptDTO);
        return Result.ok();
    }


    @ApiOperationSupport(order = 4)
    @Operation(summary = "机构删除")
    @PostMapping(value = "/delete/{id}")
    @SysLog(module = "机构模块", value = "机构删除")
    @SaCheckPermission("system:dept:delete")
    public Result<?> deleteById(@PathVariable Long id) {
        deptHandler.deleteById(id);
        return Result.ok();
    }


    @ApiOperationSupport(order = 5)
    @Operation(summary = "机构排序")
    @PostMapping(value = "/sortOrder")
    @SysLog(module = "机构模块", value = "机构排序")
    @SaCheckPermission("system:dept:edit")
    public Result<?> sortOrder(@Validated @RequestBody SortMoveRequest dto) {
        deptHandler.sortOrder(dto);
        return Result.ok();
    }


}
