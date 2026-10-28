package io.github.yilers.shortlink.controller;

import io.github.yilers.auth.api.ApiPermission;
import io.github.yilers.core.util.Result;
import io.github.yilers.shortlink.request.*;
import io.github.yilers.shortlink.response.ShortlinkResponse;
import io.github.yilers.shortlink.service.ShortlinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 外部JSON入口；签名与租户恢复在公共认证层执行，不接收用户Token作为替代。 */
@RestController
@RequestMapping("/open-api/shortlink")
@RequiredArgsConstructor
@Tag(name = "短链系统调用接口")
public class ShortlinkApiController {
    private final ShortlinkService service;

    @PostMapping("/create")
    @ApiPermission(code = "shortlink:link:create", name = "创建短链", group = "短链服务")
    @Operation(summary = "创建短链")
    public Result<ShortlinkResponse> create(@Valid @RequestBody ShortlinkCreateRequest request) {
        return Result.ok(service.create(request));
    }

    @PostMapping("/batch")
    @ApiPermission(code = "shortlink:link:create", name = "创建短链", group = "短链服务")
    @Operation(summary = "批量创建短链，整批原子提交，最多1000条")
    public Result<List<ShortlinkResponse>> batch(@Valid @RequestBody ShortlinkBatchRequest request) {
        return Result.ok(service.createBatch(request));
    }

    @Schema(description = "按幂等标识查询短链")
    public record Lookup(@NotNull Long configId, @NotBlank @Size(max = 128) String requestId) {
    }

    @PostMapping("/query")
    @ApiPermission(code = "shortlink:link:query", name = "查询短链", group = "短链服务")
    @Operation(summary = "按配置和requestId查询结果")
    public Result<ShortlinkResponse> query(@Valid @RequestBody Lookup request) {
        return Result.ok(service.find(request.configId(), request.requestId()));
    }

    @PostMapping("/renew")
    @ApiPermission(code = "shortlink:link:renew", name = "续期短链", group = "短链服务")
    @Operation(summary = "续期短链，不自动启用已停用记录")
    public Result<Void> renew(@Valid @RequestBody ShortlinkRenewRequest request) {
        service.renew(request);
        return Result.ok();
    }

    @Schema(description = "停用短链")
    public record Disable(@NotNull Long id, @NotNull Integer version) {
    }

    @PostMapping("/disable")
    @ApiPermission(code = "shortlink:link:disable", name = "停用短链", group = "短链服务")
    @Operation(summary = "停用短链")
    public Result<Void> disable(@Valid @RequestBody Disable request) {
        service.disable(request.id(), request.version());
        return Result.ok();
    }
}
