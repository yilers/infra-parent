package io.github.yilers.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.core.util.Result;
import io.github.yilers.flow.service.WorkflowService;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 申请人对本人发起且尚未办结的流程实例执行拿回或撤销。 */
@RestController
@RequestMapping("/flow/instance")
@RequiredArgsConstructor
@Tag(name = "流程实例操作")
public class WorkflowInstanceController {

    private final WorkflowService workflowService;

    @PostMapping("/take-back/{id}")
    @SaCheckPermission("flow:instance:take-back")
    @SysLog(module = "流程模块", value = "申请人拿回流程")
    @Operation(summary = "拿回本人发起的审批", description = "仅审批中的本人申请可操作；按Warm-Flow规则回到本人最近办理过的任务")
    public Result<Long> takeBack(@PathVariable Long id) {
        return Result.ok(workflowService.takeBack(id));
    }

    @PostMapping("/revoke/{id}")
    @SaCheckPermission("flow:instance:revoke")
    @SysLog(module = "流程模块", value = "申请人撤销流程")
    @Operation(summary = "撤销本人发起的申请", description = "仅本人发起且仍有待办任务的未办结流程可操作")
    public Result<Long> revoke(@PathVariable Long id) {
        return Result.ok(workflowService.revoke(id));
    }
}
