package io.github.yilers.notice.controller;

import cn.dev33.satoken.annotation.*;

import io.github.yilers.core.util.Result;
import io.github.yilers.notice.entity.*;
import io.github.yilers.notice.model.*;
import io.github.yilers.notice.model.NoticeTypes.ContentType;
import io.github.yilers.notice.request.*;
import io.github.yilers.notice.service.*;
import io.github.yilers.notice.support.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 后台发布管理；个人收件箱使用独立入口，不复用后台查看权限。 */
@RestController
@RequestMapping("/notice/message")
@RequiredArgsConstructor
@Tag(name = "站内消息管理")
public class NoticeMessageController {
    private final MessageService service;

    @GetMapping("/page")
    @SaCheckPermission("notice:message:list")
    @Operation(summary = "分页查询站内消息发布记录")
    public Result<NoticePage<NoticeMessage>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String messageType,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(service.page(keyword, messageType, current, size));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("notice:message:list")
    @Operation(summary = "查看站内消息")
    public Result<NoticeMessage> detail(@PathVariable Long id) {
        NoticeMessage notice = service.detail(id);
        if (ContentType.MARKDOWN.name().equals(notice.getContentType())) {
            notice.setRenderedContent(NoticeMarkdownRenderer.render(notice.getContent()));
        }
        return Result.ok(notice);
    }

    @PostMapping("/save")
    @SaCheckPermission("notice:message:edit")
    @Operation(summary = "保存站内消息草稿")
    public Result<Long> save(@RequestBody MessageSaveRequest request) {
        return Result.ok(service.save(request));
    }

    @PostMapping("/publish")
    @SaCheckPermission("notice:message:publish")
    @Operation(summary = "异步发布或重试失败任务", description = "提交时冻结接收名单，全部生成成功后对接收人可见")
    public Result<Void> publish(@RequestBody NoticeVersionRequest request) {
        service.publish(request);
        return Result.ok();
    }

    @PostMapping("/delete")
    @SaCheckPermission("notice:message:delete")
    @Operation(summary = "删除草稿或失败的站内消息")
    public Result<Void> delete(@RequestBody NoticeVersionRequest request) {
        service.delete(request);
        return Result.ok();
    }

    @GetMapping("/counts/{id}")
    @SaCheckPermission("notice:message:list")
    @Operation(summary = "发布人数和已读未读统计")
    public Result<Map<String, Long>> counts(@PathVariable Long id) {
        return Result.ok(service.counts(id));
    }

    @GetMapping("/recipients/{id}")
    @SaCheckPermission("notice:message:list")
    @Operation(summary = "分页查询接收名单和阅读状态")
    public Result<NoticePage<NoticeMessageRecipient>> recipients(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(service.recipients(id, read, current, size));
    }
}
