package io.github.yilers.infra.flow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.core.util.Result;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 单体部署下为转办、委派提供当前租户内的有效用户目录。 */
@RestController
@RequestMapping("/flow/task")
@RequiredArgsConstructor
@Tag(name = "流程任务办理人目录")
public class WorkflowTaskDirectoryController {

    private final UserService userService;
    private final WorkflowIdentityProvider identityProvider;

    @GetMapping("/target-users")
    @SaCheckPermission(value = {"flow:task:transfer", "flow:task:delegate"}, mode = SaMode.OR)
    @Operation(summary = "分页搜索转办或委派目标用户", description = "仅返回当前租户启用用户；至少需要转办或委派权限之一")
    public Result<Page<TaskUserCandidate>> targetUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("用户分页参数不合法");
        }
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("当前租户上下文不能为空");
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<User> users = userService.lambdaQuery()
                .eq(User::getTenantId, tenantId)
                .eq(User::getUsable, CommonConst.YES)
                .ne(User::getId, Long.valueOf(identityProvider.currentUserId()))
                .and(normalizedKeyword != null, query -> query
                        .like(User::getName, normalizedKeyword)
                        .or().like(User::getAccount, normalizedKeyword))
                .orderByAsc(User::getName)
                .page(new Page<>(pageNum, pageSize));
        List<TaskUserCandidate> records = users.getRecords().stream()
                .map(user -> new TaskUserCandidate(user.getId().toString(),
                        user.getName() == null || user.getName().isBlank() ? user.getAccount() : user.getName(),
                        user.getAccount()))
                .toList();
        Page<TaskUserCandidate> result = new Page<>(users.getCurrent(), users.getSize(), users.getTotal());
        result.setRecords(records);
        return Result.ok(result);
    }

    @Schema(description = "流程任务目标用户，仅包含转办或委派所需的最小展示字段")
    public record TaskUserCandidate(
            @Schema(description = "用户ID") String id,
            @Schema(description = "姓名；未填写姓名时回退显示账号") String name,
            @Schema(description = "登录账号") String account) {
    }
}
