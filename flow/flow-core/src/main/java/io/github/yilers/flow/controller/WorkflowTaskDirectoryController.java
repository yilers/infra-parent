package io.github.yilers.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.core.util.Result;
import io.github.yilers.flow.model.WorkflowDirectoryEntry;
import io.github.yilers.flow.model.WorkflowDirectoryPage;
import io.github.yilers.flow.service.WorkflowDirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Flow 提供的转办、委派目标用户搜索接口。 */
@RestController
@RequestMapping("/flow/task")
@Tag(name = "流程任务办理人目录")
public class WorkflowTaskDirectoryController {

    private final WorkflowDirectoryService directoryService;

    public WorkflowTaskDirectoryController(WorkflowDirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping("/target-users")
    @SaCheckPermission(value = {"flow:task:transfer", "flow:task:delegate"}, mode = SaMode.OR)
    @Operation(summary = "分页搜索转办或委派目标用户", description = "仅返回当前租户启用用户；至少需要转办或委派权限之一")
    public Result<Page<TaskUserCandidate>> targetUsers(@RequestParam(required = false) String keyword,
                                                       @RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize) {
        WorkflowDirectoryPage directoryPage = directoryService.searchTaskTargets(keyword, pageNum, pageSize);
        Page<TaskUserCandidate> page = new Page<>(directoryPage.current(), directoryPage.size(), directoryPage.total());
        List<TaskUserCandidate> records = directoryPage.records().stream()
                .map(WorkflowTaskDirectoryController::toCandidate).toList();
        page.setRecords(records);
        return Result.ok(page);
    }

    private static TaskUserCandidate toCandidate(WorkflowDirectoryEntry entry) {
        return new TaskUserCandidate(entry.id(), entry.name(), entry.code());
    }

    @Schema(description = "流程任务目标用户，仅包含转办或委派所需的最小展示字段")
    public record TaskUserCandidate(
            @Schema(description = "用户ID") String id,
            @Schema(description = "姓名；未填写姓名时回退显示账号") String name,
            @Schema(description = "登录账号") String account) {
    }
}
