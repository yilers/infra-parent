package io.github.yilers.shortlink.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.core.util.Result;
import io.github.yilers.shortlink.entity.*;
import io.github.yilers.shortlink.mapper.ShortlinkLinkMapper;
import io.github.yilers.shortlink.request.*;
import io.github.yilers.shortlink.response.ShortlinkResponse;
import io.github.yilers.shortlink.service.*;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

/** 管理端使用用户Token和菜单按钮权限，与机器接口分离。 */
@RestController
@RequestMapping("/shortlink")
@RequiredArgsConstructor
@Tag(name = "短链管理")
public class ShortlinkManagementController {
    private final ShortlinkManagementService management;
    private final ShortlinkService service;
    private final ShortlinkLinkMapper links;

    @GetMapping("/config/list")
    @SaCheckPermission("shortlink:config:list")
    @Operation(summary = "查询短链配置")
    public Result<List<ShortlinkConfig>> configs() { return Result.ok(management.configs()); }

    @PostMapping("/config/save")
    @SysLog(module = "短链管理", value = "保存短链配置", recordParams = false)
    @SaCheckPermission("shortlink:config:edit")
    @Operation(summary = "创建或编辑短链配置")
    public Result<Long> config(@Valid @RequestBody ShortlinkConfigRequest request) { return Result.ok(management.saveConfig(request)); }

    @GetMapping("/domain/list")
    @SaCheckPermission("shortlink:domain:list")
    @Operation(summary = "查询当前租户域名")
    public Result<List<ShortlinkDomain>> domains() { return Result.ok(management.domains()); }

    @PostMapping("/domain/save")
    @SysLog(module = "短链管理", value = "保存短链域名", recordParams = false)
    @SaCheckPermission("shortlink:domain:edit")
    @Operation(summary = "创建或启停域名")
    public Result<Long> domain(@Valid @RequestBody ShortlinkDomainRequest request) { return Result.ok(management.saveDomain(request)); }

    @GetMapping("/namespace/list/{configId}")
    @SaCheckPermission("shortlink:config:list")
    @Operation(summary = "查询配置下的namespace")
    public Result<List<ShortlinkNamespace>> namespaces(@PathVariable Long configId) { return Result.ok(management.namespaces(configId)); }

    @PostMapping("/namespace/save")
    @SysLog(module = "短链管理", value = "保存短链路径", recordParams = false)
    @SaCheckPermission("shortlink:config:edit")
    @Operation(summary = "创建或启停namespace，路径及生成规则不可编辑")
    public Result<Long> namespace(@Valid @RequestBody ShortlinkNamespaceRequest request) {
        return Result.ok(management.saveNamespace(request));
    }

    @Schema(description = "配置授权请求")
    public record Grant(@NotNull Long configId, @NotNull Integer version, @NotNull @Size(max = 200) Set<@NotNull Long> credentialIds) {
    }

    @GetMapping("/config/grants/{id}")
    @SaCheckPermission("shortlink:config:grant")
    @Operation(summary = "查询配置已授权的凭证ID")
    public Result<List<Long>> grants(@PathVariable Long id) { return Result.ok(management.grants(id)); }

    @PostMapping("/config/grant")
    @SysLog(module = "短链管理", value = "修改短链配置调用授权", recordParams = false)
    @SaCheckPermission("shortlink:config:grant")
    @Operation(summary = "替换配置授权；只允许当前租户凭证")
    public Result<Void> grant(@Valid @RequestBody Grant request) {
        management.grant(request.configId(), request.version(), request.credentialIds());
        return Result.ok();
    }

    @GetMapping("/link/page")
    @SaCheckPermission("shortlink:link:list")
    @Operation(summary = "分页查询短链，可按配置、短码或目标地址过滤")
    public Result<Page<ShortlinkLink>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "20") long size,
                                          @RequestParam(required = false) Long configId,
                                          @RequestParam(required = false) @Size(max = 128) String batchId,
                                          @RequestParam(required = false) String keyword) {
        ShortlinkServiceImpl.tenant();
        Page<ShortlinkLink> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        var query = new LambdaQueryWrapper<ShortlinkLink>().eq(configId != null, ShortlinkLink::getConfigId, configId)
                .eq(batchId != null && !batchId.isBlank(), ShortlinkLink::getBatchId, batchId)
                .orderByDesc(ShortlinkLink::getId);
        if (keyword != null && !keyword.isBlank()) {
            if (keyword.length() > 200) throw new io.github.yilers.web.exception.CommonException("搜索内容不能超过200字");
            query.and(where -> where.like(ShortlinkLink::getTargetUrl, keyword).or().eq(ShortlinkLink::getShortCode, keyword));
        }
        return Result.ok(links.selectPage(page, query));
    }

    @Schema(description = "短链页面所需的配置、域名和路径选项")
    public record LinkOptions(List<ShortlinkConfig> configs, List<ShortlinkDomain> domains,
                              List<ShortlinkNamespace> namespaces) {
    }

    @GetMapping("/link/options")
    @SaCheckPermission("shortlink:link:list")
    @Operation(summary = "查询当前租户短链页面选项")
    public Result<LinkOptions> options() {
        List<ShortlinkConfig> configs = management.configs();
        List<ShortlinkNamespace> namespaces = configs.stream()
                .flatMap(config -> management.namespaces(config.getId()).stream()).toList();
        return Result.ok(new LinkOptions(configs, management.domains(), namespaces));
    }

    @PostMapping("/link/create")
    @SysLog(module = "短链管理", value = "创建短链", recordParams = false)
    @SaCheckPermission("shortlink:link:add")
    @Operation(summary = "管理端创建短链")
    public Result<ShortlinkResponse> create(@Valid @RequestBody ShortlinkCreateRequest request) { return Result.ok(service.create(request)); }

    @PostMapping("/link/renew")
    @SysLog(module = "短链管理", value = "续期短链", recordParams = false)
    @SaCheckPermission("shortlink:link:renew")
    @Operation(summary = "管理端续期短链")
    public Result<Void> renew(@Valid @RequestBody ShortlinkRenewRequest request) {
        service.renew(request);
        return Result.ok();
    }

    @PostMapping("/link/disable")
    @SysLog(module = "短链管理", value = "停用短链", recordParams = false)
    @SaCheckPermission("shortlink:link:disable")
    @Operation(summary = "管理端停用短链")
    public Result<Void> disable(@Valid @RequestBody ShortlinkApiController.Disable request) {
        service.disable(request.id(), request.version());
        return Result.ok();
    }

    @PostMapping("/link/enable")
    @SysLog(module = "短链管理", value = "启用短链", recordParams = false)
    @SaCheckPermission("shortlink:link:enable")
    @Operation(summary = "管理端启用短链，不改变原有效期")
    public Result<Void> enable(@Valid @RequestBody ShortlinkApiController.Disable request) {
        service.enable(request.id(), request.version());
        return Result.ok();
    }

    @PostMapping("/link/delete")
    @SysLog(module = "短链管理", value = "删除短链", recordParams = false)
    @SaCheckPermission("shortlink:link:delete")
    @Operation(summary = "逻辑删除短链，短码和请求标识不可重用")
    public Result<Void> delete(@Valid @RequestBody ShortlinkApiController.Disable request) {
        service.delete(request.id(), request.version());
        return Result.ok();
    }
}
