package io.github.yilers.flow.leave.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.github.yilers.core.util.Result;
import io.github.yilers.flow.leave.model.LeaveInfo;
import io.github.yilers.flow.leave.model.LeavePage;
import io.github.yilers.flow.leave.request.LeaveSaveRequest;
import io.github.yilers.flow.leave.request.LeaveVersionRequest;
import io.github.yilers.flow.leave.service.LeaveService;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 请假申请入口；菜单按钮鉴权之外，Service始终校验本人草稿或流程参与身份。 */
@RestController
@ConditionalOnProperty(prefix = "flow.leave", name = "enabled", havingValue = "true")
@RequestMapping("/flow/leave")
@RequiredArgsConstructor
@Tag(name = "请假申请")
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping("/page")
    @SaCheckPermission("flow:leave:list")
    @Operation(summary = "查询本人请假申请", description = "仅当前租户本人记录；status为DRAFT或引擎状态编码，页大小最多100")
    public Result<LeavePage> page(@RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "1") int pageNum,
                                  @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(leaveService.page(status, pageNum, pageSize));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission(value = {"flow:leave:list", "flow:task:todo", "flow:task:done", "flow:instance:mine"}, mode = SaMode.OR)
    @Operation(summary = "查看请假单正文", description = "本人可查看；非本人仅在属于该实例候选人或历史办理人时可查看，不能浏览他人草稿")
    public Result<LeaveInfo> detail(@PathVariable Long id) {
        return Result.ok(leaveService.detail(id));
    }

    @PostMapping("/add")
    @SaCheckPermission("flow:leave:add")
    @Operation(summary = "新建请假草稿", description = "只保存业务数据，不启动流程；申请人与租户由登录态确定")
    public Result<LeaveInfo> add(@Valid @RequestBody LeaveSaveRequest request) {
        return Result.ok(leaveService.create(request));
    }

    @PostMapping("/edit/{id}")
    @SaCheckPermission("flow:leave:edit")
    @Operation(summary = "编辑本人请假草稿", description = "必须携带version，提交后不可通过本接口修改")
    public Result<LeaveInfo> edit(@PathVariable Long id, @Valid @RequestBody LeaveSaveRequest request) {
        return Result.ok(leaveService.edit(id, request));
    }

    @PostMapping("/submit/{id}")
    @SaCheckPermission("flow:leave:submit")
    @SysLog(module = "请假模块", value = "提交审批")
    @Operation(summary = "提交本人请假草稿", description = "锁定草稿后启动已发布的leave流程并绑定实例，任一步失败整笔回滚")
    public Result<Long> submit(@PathVariable Long id, @Valid @RequestBody LeaveVersionRequest request) {
        return Result.ok(leaveService.submit(id, request.version()));
    }

    @PostMapping("/delete/{id}")
    @SaCheckPermission("flow:leave:delete")
    @SysLog(module = "请假模块", value = "删除草稿")
    @Operation(summary = "删除本人请假草稿", description = "仅未提交草稿可物理删除，需携带当前version")
    public Result<?> delete(@PathVariable Long id, @Valid @RequestBody LeaveVersionRequest request) {
        leaveService.deleteDraft(id, request.version());
        return Result.ok();
    }
}
