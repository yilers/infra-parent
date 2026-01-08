package com.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yilers.core.util.Result;
import com.github.yilers.upm.entity.Position;
import com.github.yilers.upm.request.PositionRequest;
import com.github.yilers.upm.service.PositionService;
import com.github.yilers.web.log.SysLog;
import com.github.yilers.web.validated.Add;
import com.github.yilers.web.validated.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;

@RestController
@RequestMapping("/position")
@RequiredArgsConstructor
@Tag(name = "职位")
public class PositionController {
    private final PositionService positionService;

    @Operation(summary = "职位查询所有")
    @GetMapping("/findAll")
//    @SaCheckPermission("system:position:list")
    public Result<List<Position>> findAll() {
        List<Position> list = positionService.list(Wrappers.lambdaQuery(Position.class)
                .orderByAsc(Position::getSortNumber)
                .orderByAsc(Position::getId));
        return Result.ok(list);
    }

    @Operation(summary = "职位保存")
    @PostMapping("/save")
    @SysLog(module = "职位模块", value = "新增职位")
    @SaCheckPermission("system:position:add")
    public Result<?> save(@Validated(Add.class) @RequestBody PositionRequest dto) {
        positionService.savePosition(dto);
        return Result.ok();
    }

    @Operation(summary = "职位根据id更新")
    @PostMapping("/updateById")
    @SysLog(module = "职位模块", value = "更新职位")
    @SaCheckPermission("system:position:edit")
    public Result<?> updateById(@Validated(Update.class) @RequestBody PositionRequest dto) {
        positionService.updatePosition(dto);
        return Result.ok();
    }

    @Operation(summary = "职位通过id查询")
    @GetMapping("/findById/{id}")
    public Result<Position> findById(@PathVariable("id") Serializable id) {
        return Result.ok(positionService.getById(id));
    }

    @Operation(summary = "职位根据id删除")
    @PostMapping("/deleteById/{id}")
    @SysLog(module = "职位模块", value = "删除职位")
    @SaCheckPermission("system:position:delete")
    public Result<?> deleteById(@PathVariable("id") Serializable id) {
        positionService.deleteById(id);
        return Result.ok();
    }
}
