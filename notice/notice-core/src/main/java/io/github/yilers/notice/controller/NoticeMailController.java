package io.github.yilers.notice.controller;

import cn.dev33.satoken.annotation.*;

import io.github.yilers.core.util.Result;
import io.github.yilers.notice.entity.*;
import io.github.yilers.notice.model.*;
import io.github.yilers.notice.request.*;
import io.github.yilers.notice.service.*;
import io.github.yilers.notice.support.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 邮件任务管理，SMTP凭据不出现在任何响应中。 */
@RestController
@RequestMapping("/notice/mail")
@RequiredArgsConstructor
@Tag(name = "邮件发送管理")
public class NoticeMailController {
    private final MailService service;

    @GetMapping("/page")
    @SaCheckPermission("notice:mail:list")
    @Operation(summary = "查询邮件任务")
    public Result<NoticePage<NoticeMail>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(service.page(keyword, current, size));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("notice:mail:list")
    @Operation(summary = "查看邮件内容快照")
    public Result<NoticeMail> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @GetMapping("/preview/{id}")
    @SaCheckPermission("notice:mail:list")
    @Operation(summary = "按首个收件人预览已保存的邮件快照")
    public Result<MailPreview> preview(@PathVariable Long id) {
        return Result.ok(service.preview(id));
    }

    @GetMapping("/recipients/{id}")
    @SaCheckPermission("notice:mail:list")
    @Operation(summary = "查询收件结果及重试次数")
    public Result<NoticePage<NoticeMailRecipient>> recipients(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(service.recipients(id, current, size));
    }

    @PostMapping("/save")
    @SaCheckPermission("notice:mail:edit")
    @Operation(summary = "保存邮件任务，支持外部邮箱及个人变量")
    public Result<Long> save(@RequestBody MailSaveRequest request) {
        return Result.ok(service.save(request));
    }

    @PostMapping("/send")
    @SaCheckPermission("notice:mail:send")
    @Operation(summary = "提交邮件异步发送")
    public Result<Void> send(@RequestBody NoticeVersionRequest request) {
        service.send(request);
        return Result.ok();
    }

    @PostMapping("/retry")
    @SaCheckPermission("notice:mail:send")
    @Operation(summary = "只重试失败收件人，保留累计尝试次数")
    public Result<Void> retry(@RequestBody NoticeVersionRequest request) {
        service.retry(request);
        return Result.ok();
    }

    @PostMapping("/delete")
    @SaCheckPermission("notice:mail:delete")
    @Operation(summary = "删除邮件草稿")
    public Result<Void> delete(@RequestBody NoticeVersionRequest request) {
        service.delete(request);
        return Result.ok();
    }
}
