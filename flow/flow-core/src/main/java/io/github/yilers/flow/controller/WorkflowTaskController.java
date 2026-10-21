package io.github.yilers.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.core.util.Result;
import io.github.yilers.flow.request.WorkflowTaskActionRequest;
import io.github.yilers.flow.request.WorkflowTaskAssignRequest;
import io.github.yilers.flow.service.WorkflowService;
import io.github.yilers.web.exception.CommonException;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 菜单按钮权限与WorkflowService内的当前任务办理人校验同时生效。 */
@RestController
@RequestMapping("/flow/task")
@RequiredArgsConstructor
@Tag(name = "流程任务办理")
public class WorkflowTaskController {

    private final WorkflowService workflowService;

    @PostMapping("/pass/{id}")
    @SaCheckPermission("flow:task:pass")
    @SysLog(module = "流程模块", value = "审批通过")
    @Operation(summary = "通过本人当前待办", description = "服务端锁定实例并重新检查任务办理人；不接受客户端修改流程变量")
    public Result<Long> pass(@PathVariable Long id, @Valid @RequestBody WorkflowTaskActionRequest request) {
        return Result.ok(workflowService.pass(id, normalize(request.message()), Map.of()));
    }

    @PostMapping("/reject/{id}")
    @SaCheckPermission("flow:task:reject")
    @SysLog(module = "流程模块", value = "审批退回")
    @Operation(summary = "退回本人当前待办", description = "必须填写意见；按设计器配置的退回路线执行，不是直接终止流程")
    public Result<Long> reject(@PathVariable Long id, @Valid @RequestBody WorkflowTaskActionRequest request) {
        String message = normalize(request.message());
        if (message == null) {
            throw new CommonException("请填写退回原因");
        }
        return Result.ok(workflowService.reject(id, message, Map.of()));
    }

    @PostMapping("/transfer/{id}")
    @SaCheckPermission("flow:task:transfer")
    @SysLog(module = "流程模块", value = "待办转办")
    @Operation(summary = "将本人当前待办转办给同租户用户")
    public Result<Long> transfer(@PathVariable Long id, @Valid @RequestBody WorkflowTaskAssignRequest request) {
        return Result.ok(workflowService.transfer(id, request.targetUserId(), normalize(request.message())));
    }

    @PostMapping("/delegate/{id}")
    @SaCheckPermission("flow:task:delegate")
    @SysLog(module = "流程模块", value = "待办委派")
    @Operation(summary = "将本人当前待办委派给同租户用户", description = "受托人办理后任务按Warm-Flow规则返回委派人继续办理")
    public Result<Long> delegate(@PathVariable Long id, @Valid @RequestBody WorkflowTaskAssignRequest request) {
        return Result.ok(workflowService.delegate(id, request.targetUserId(), normalize(request.message())));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
