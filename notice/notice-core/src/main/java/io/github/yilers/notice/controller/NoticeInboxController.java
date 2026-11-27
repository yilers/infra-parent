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

/** 当前登录人的个人消息，仅依赖登录态和接收关系，不要求通知管理菜单。 */
@RestController
@RequestMapping("/notice/inbox")
@SaCheckLogin
@RequiredArgsConstructor
@Tag(name = "个人消息中心")
public class NoticeInboxController {
    private final NoticeInboxService service;

    @GetMapping("/page")
    @Operation(summary = "我的消息列表")
    public Result<NoticePage<NoticeInboxItem>> page(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(service.page(type, unread, current, size));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "查看我收到的消息")
    public Result<NoticeInboxItem> detail(@PathVariable Long id) {
        NoticeInboxItem item = service.detail(id);
        if (ContentType.MARKDOWN.name().equals(item.contentType())) {
            // 收件详情直接返回可阅读的 HTML，原始 Markdown 仍存放在通知记录。
            item = new NoticeInboxItem(
                    item.id(), item.messageId(), item.title(), item.messageType(),
                    NoticeMarkdownRenderer.render(item.content()), ContentType.HTML.name(), item.actionUrl(),
                    item.readTime(), item.publishTime());
        }
        return Result.ok(item);
    }

    @GetMapping("/unread")
    @Operation(summary = "按分类统计我的未读消息")
    public Result<Map<String, Long>> unread() {
        return Result.ok(service.unread());
    }

    @PostMapping("/read/{id}")
    @Operation(summary = "标记本人消息已读")
    public Result<Void> read(@PathVariable Long id) {
        service.read(id);
        return Result.ok();
    }

    @PostMapping("/remove/{id}")
    @Operation(summary = "从个人列表移除消息，不影响发布统计")
    public Result<Void> remove(@PathVariable Long id) {
        service.remove(id);
        return Result.ok();
    }

    @PostMapping("/readAll")
    @Operation(summary = "将本人可见消息全部标记为已读")
    public Result<Void> readAll() {
        service.readAll();
        return Result.ok();
    }
}
