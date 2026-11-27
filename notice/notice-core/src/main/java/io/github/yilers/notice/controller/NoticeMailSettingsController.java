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

/** 租户邮件账号、模板和安全预览入口。 */
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
@Tag(name = "邮件配置与模板")
public class NoticeMailSettingsController {
    private final MailConfigurationService configurations;
    private final MailRenderer renderer;
    private final SmtpMailSender sender;

    @GetMapping("/mail-config/list")
    @SaCheckPermission(
            value = {"notice:mail-config:list", "notice:mail:edit", "notice:mail-template:test"},
            mode = SaMode.OR)
    @Operation(summary = "查询当前租户邮件配置，密码不返回")
    public Result<List<NoticeMailConfig>> configs() {
        return Result.ok(configurations.configs());
    }

    @PostMapping("/mail-config/save")
    @SaCheckPermission("notice:mail-config:edit")
    @Operation(summary = "保存SMTP配置，密码为空保留原值")
    public Result<Long> saveConfig(@RequestBody MailConfigSaveRequest request) {
        return Result.ok(configurations.saveConfig(request));
    }

    @PostMapping("/mail-config/delete")
    @SaCheckPermission("notice:mail-config:delete")
    @Operation(summary = "删除SMTP配置")
    public Result<Void> deleteConfig(@RequestBody NoticeVersionRequest request) {
        configurations.deleteConfig(request);
        return Result.ok();
    }

    @GetMapping("/mail-template/list")
    @SaCheckPermission(
            value = {"notice:mail-template:list", "notice:mail:edit"},
            mode = SaMode.OR)
    @Operation(summary = "查询租户邮件模板")
    public Result<List<NoticeMailTemplate>> templates() {
        return Result.ok(configurations.templates());
    }

    @PostMapping("/mail-template/save")
    @SaCheckPermission("notice:mail-template:edit")
    @Operation(summary = "保存HTML、文本或Markdown模板")
    public Result<Long> saveTemplate(@RequestBody MailTemplateSaveRequest request) {
        return Result.ok(configurations.saveTemplate(request));
    }

    @PostMapping("/mail-template/delete")
    @SaCheckPermission("notice:mail-template:delete")
    @Operation(summary = "删除邮件模板")
    public Result<Void> deleteTemplate(@RequestBody NoticeVersionRequest request) {
        configurations.deleteTemplate(request);
        return Result.ok();
    }

    @PostMapping("/mail-template/preview")
    @SaCheckPermission(
            value = {"notice:mail-template:edit", "notice:mail:edit"},
            mode = SaMode.OR)
    @Operation(summary = "渲染邮件模板预览，不执行脚本")
    public Result<MailPreview> preview(@RequestBody MailPreviewRequest request) {
        return Result.ok(renderer.render(request));
    }

    @PostMapping("/mail-template/test")
    @SaCheckPermission("notice:mail-template:test")
    @Operation(summary = "发送一封真实测试邮件")
    public Result<Void> test(@RequestBody MailTestRequest request) {
        var config = configurations.config(request.configId());
        var preview = renderer.render(request.preview());
        try {
            sender.send(
                    config,
                    config.getFromAddress(),
                    config.getFromName(),
                    config.getReplyTo(),
                    NoticeSupport.email(request.email()),
                    preview,
                    null);
        } catch (Exception e) {
            throw new io.github.yilers.web.exception.CommonException(
                    io.github.yilers.notice.i18n.NoticeMessageCode.TEST_FAILED);
        }
        return Result.ok();
    }
}
