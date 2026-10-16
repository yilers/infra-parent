package io.github.yilers.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.github.yilers.core.util.Result;
import io.github.yilers.flow.model.WorkflowDefinitionInfo;
import io.github.yilers.flow.model.WorkflowDefinitionPage;
import io.github.yilers.flow.service.WorkflowDefinitionService;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 流程定义管理接口。只开放定义管理，不开放流程发起或任务办理。
 * 设计内容采用 Warm-Flow 1.8.9 原生 JSON，便于后续对接官方 Vue 设计器。
 */
@RestController
@RequestMapping("/flow/definition")
@RequiredArgsConstructor
@Tag(name = "流程定义管理")
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService definitionService;

    @GetMapping("/page")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "分页查询流程定义")
    public Result<WorkflowDefinitionPage> page(
            @Parameter(description = "流程编码，精确匹配") @RequestParam(required = false) String flowCode,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(definitionService.page(flowCode, pageNum, pageSize));
    }

    @GetMapping("/versions/{flowCode}")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "查询流程全部版本")
    public Result<List<WorkflowDefinitionInfo>> versions(@PathVariable String flowCode) {
        return Result.ok(definitionService.versions(flowCode));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "查询流程定义摘要")
    public Result<WorkflowDefinitionInfo> detail(@PathVariable Long id) {
        return Result.ok(definitionService.get(id));
    }

    @GetMapping("/design/{id}")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "获取流程设计 JSON")
    public Result<String> design(@PathVariable Long id) {
        return Result.ok(definitionService.exportJson(id));
    }

    @PostMapping("/add")
    @SaCheckPermission("flow:definition:add")
    @SysLog(module = "流程模块", value = "新建流程定义")
    @Operation(summary = "新建未发布流程定义", description = "请求体为 Warm-Flow 1.8.9 设计器 JSON；服务端忽略发布状态")
    public Result<Long> add(@RequestBody String designJson) {
        return Result.ok(definitionService.create(designJson));
    }

    @PostMapping("/edit/{id}")
    @SaCheckPermission("flow:definition:edit")
    @SysLog(module = "流程模块", value = "修改流程定义")
    @Operation(summary = "修改未发布且无实例的流程定义", description = "请求体为 Warm-Flow 1.8.9 设计器 JSON，ID、编码和版本须与原定义一致")
    public Result<?> edit(@PathVariable Long id, @RequestBody String designJson) {
        definitionService.update(id, designJson);
        return Result.ok();
    }

    @PostMapping("/copy/{id}")
    @SaCheckPermission("flow:definition:add")
    @SysLog(module = "流程模块", value = "复制流程版本")
    @Operation(summary = "复制为新的未发布版本")
    public Result<?> copy(@PathVariable Long id) {
        definitionService.copy(id);
        return Result.ok();
    }

    @PostMapping("/publish/{id}")
    @SaCheckPermission("flow:definition:publish")
    @SysLog(module = "流程模块", value = "发布流程定义")
    @Operation(summary = "发布指定流程版本")
    public Result<Boolean> publish(@PathVariable Long id) {
        return Result.ok(definitionService.publish(id));
    }

    @PostMapping("/unpublish/{id}")
    @SaCheckPermission("flow:definition:publish")
    @SysLog(module = "流程模块", value = "取消发布流程定义")
    @Operation(summary = "取消发布指定流程版本", description = "已有流程实例时 Warm-Flow 会拒绝取消发布")
    public Result<Boolean> unpublish(@PathVariable Long id) {
        return Result.ok(definitionService.unpublish(id));
    }

    @PostMapping("/delete/{id}")
    @SaCheckPermission("flow:definition:delete")
    @SysLog(module = "流程模块", value = "删除流程定义")
    @Operation(summary = "删除未发布且无实例的流程版本")
    public Result<?> delete(@PathVariable Long id) {
        definitionService.delete(id);
        return Result.ok();
    }
}
