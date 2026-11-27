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

/** 通知接收对象选择器，只开放给有发布编辑权限的人员。 */
@RestController
@RequestMapping("/notice/directory")
@RequiredArgsConstructor
@Tag(name = "通知接收对象")
public class NoticeDirectoryController {
    private final NoticeDataProvider provider;

    @GetMapping("/labels")
    @SaCheckPermission(
            value = {"notice:message:edit", "notice:mail:edit"},
            mode = SaMode.OR)
    @Operation(summary = "回显当前租户已选择的通知接收对象")
    public Result<List<NoticeDirectoryEntry>> labels(
            @RequestParam NoticeTypes.DirectoryType type, @RequestParam List<Long> ids) {
        NoticeSupport.tenant();
        NoticeSupport.require(
                ids.size() <= 500 && ids.stream().allMatch(id -> id != null && id > 0),
                io.github.yilers.notice.i18n.NoticeMessageCode.INVALID,
                "ids");
        return Result.ok(provider.findEntries(type, ids));
    }

    @GetMapping
    @SaCheckPermission(
            value = {"notice:message:edit", "notice:mail:edit"},
            mode = SaMode.OR)
    @Operation(summary = "分页选择当前租户用户、部门和角色")
    public Result<NoticePage<NoticeDirectoryEntry>> search(
            @RequestParam NoticeTypes.DirectoryType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        NoticeSupport.tenant();
        NoticeSupport.page(current, size);
        return Result.ok(provider.search(type, keyword, current, size));
    }
}
