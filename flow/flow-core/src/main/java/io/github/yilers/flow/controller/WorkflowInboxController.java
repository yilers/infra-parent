package io.github.yilers.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.github.yilers.core.util.Result;
import io.github.yilers.flow.model.WorkflowInstanceDetail;
import io.github.yilers.flow.model.WorkflowPage;
import io.github.yilers.flow.service.WorkflowInboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 审批中心入口。菜单权限与Service内的实例参与人授权分别校验。 */
@RestController
@RequestMapping("/flow/inbox")
@RequiredArgsConstructor
@Tag(name = "审批中心")
public class WorkflowInboxController {

    private final WorkflowInboxService inboxService;

    @GetMapping("/todo")
    @SaCheckPermission("flow:task:todo")
    @Operation(summary = "分页查询我的待办", description = "仅当前租户、本人可办理的审批节点；流程编码精确匹配")
    public Result<WorkflowPage> todo(@RequestParam(required = false) String flowCode,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(inboxService.todo(flowCode, pageNum, pageSize));
    }

    @GetMapping("/done")
    @SaCheckPermission("flow:task:done")
    @Operation(summary = "分页查询我的已办", description = "仅本人实际办理的审批记录，不包含发起记录")
    public Result<WorkflowPage> done(@RequestParam(required = false) String flowCode,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(inboxService.done(flowCode, pageNum, pageSize));
    }

    @GetMapping("/started")
    @SaCheckPermission("flow:instance:mine")
    @Operation(summary = "分页查询我发起的流程")
    public Result<WorkflowPage> started(@RequestParam(required = false) String flowCode,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(inboxService.started(flowCode, pageNum, pageSize));
    }

    @GetMapping("/cc")
    @SaCheckPermission("flow:cc:list")
    @Operation(summary = "分页查询抄送我的", description = "只返回本租户实际接收到的节点抄送，不生成审批待办")
    public Result<WorkflowPage> cc(@RequestParam(required = false) String flowCode,
                                   @RequestParam(defaultValue = "1") int pageNum,
                                   @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(inboxService.cc(flowCode, pageNum, pageSize));
    }

    @PostMapping("/cc/read/{id}")
    @SaCheckPermission("flow:cc:list")
    @Operation(summary = "标记抄送已读", description = "仅接收人本人可操作，重复调用保持幂等")
    public Result<Void> readCc(@PathVariable Long id) {
        inboxService.readCc(id);
        return Result.ok();
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission(value = {"flow:task:todo", "flow:task:done", "flow:instance:mine", "flow:cc:list"}, mode = SaMode.OR)
    @Operation(summary = "查看与本人相关的流程详情", description = "仅发起人、历史办理人、当前任务候选人或抄送接收人可查看；不返回业务正文及流程变量")
    public Result<WorkflowInstanceDetail> detail(@PathVariable Long id) {
        return Result.ok(inboxService.detail(id));
    }
}
