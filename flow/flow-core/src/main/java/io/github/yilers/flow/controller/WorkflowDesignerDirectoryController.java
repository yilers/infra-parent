package io.github.yilers.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.core.util.Result;
import io.github.yilers.flow.service.WorkflowDirectoryService;
import io.github.yilers.flow.service.WorkflowDirectoryService.HandlerDict;
import io.github.yilers.flow.service.WorkflowDirectoryService.HandlerFeedback;
import io.github.yilers.flow.service.WorkflowDirectoryService.HandlerResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Flow 提供的设计器办理人目录接口。
 * <p>
 * 组织数据查询委托给宿主实现的 {@code WorkflowDataProvider}，本 Controller 不依赖 UPM。
 */
@RestController
@RequestMapping("/flow/designer")
@Tag(name = "流程设计器组织目录")
public class WorkflowDesignerDirectoryController {

    private final WorkflowDirectoryService directoryService;

    public WorkflowDesignerDirectoryController(WorkflowDirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping("/handler-type")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "获取办理人类型")
    public Result<List<String>> handlerType() {
        return Result.ok(directoryService.handlerTypes());
    }

    @GetMapping("/handler-dict")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "获取办理人类型字典")
    public Result<List<HandlerDict>> handlerDict() {
        return Result.ok(directoryService.handlerDictionary());
    }

    @GetMapping("/handler-result")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "分页搜索当前租户的流程办理人",
            description = "支持角色、部门、用户及动态规则；动态规则只返回配置选项，实际审批人在发起流程时由服务端解析")
    public Result<HandlerResult> handlerResult(@RequestParam(defaultValue = "角色") String handlerType,
                                               @RequestParam(required = false) String userName,
                                               @RequestParam(required = false) Long groupId,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(directoryService.searchHandlers(handlerType, userName, groupId, pageNum, pageSize));
    }

    @GetMapping("/handler-feedback")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "回显已配置的办理人名称")
    public Result<List<HandlerFeedback>> handlerFeedback(@RequestParam(required = false) String storageIds) {
        return Result.ok(directoryService.resolveHandlerFeedback(storageIds));
    }
}
