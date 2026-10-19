package io.github.yilers.infra.flow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.yilers.core.util.Result;
import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 单体启动器提供的流程设计器组织目录适配。
 * <p>
 * Flow 引擎不依赖 UPM；未来独立部署时可由业务启动器提供同形接口。
 * 返回的 storageId 与 {@link UpmWorkflowIdentityProvider} 的办理人标识保持一致。
 */
@RestController
@RequestMapping("/flow/designer")
@RequiredArgsConstructor
@Tag(name = "流程设计器组织目录")
public class WorkflowDesignerDirectoryController {

    private final UserService userService;
    private final RoleService roleService;
    private final DeptService deptService;

    @GetMapping("/handler-type")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "获取办理人类型")
    public Result<List<String>> handlerType() {
        return Result.ok(List.of("角色", "部门", "用户", "动态规则"));
    }

    @GetMapping("/handler-dict")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "获取办理人类型字典")
    public Result<List<HandlerDict>> handlerDict() {
        return Result.ok(List.of(new HandlerDict("角色", "role", List.of()),
                new HandlerDict("部门", "dept", List.of()), new HandlerDict("用户", "user", List.of()),
                new HandlerDict("动态规则", "dynamic", List.of())));
    }

    @GetMapping("/handler-result")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "分页搜索当前租户的流程办理人", description = "支持角色、部门、用户及动态规则；动态规则只返回配置选项，实际审批人在发起流程时由服务端解析")
    public Result<HandlerResult> handlerResult(@RequestParam(defaultValue = "角色") String handlerType,
                                               @RequestParam(required = false) String userName,
                                               @RequestParam(required = false) Long groupId,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("办理人分页参数不合法");
        }
        // 动态规则是配置选项而非组织成员；不查询或展示某个具体用户的主管信息。
        if ("动态规则".equals(handlerType)) {
            List<HandlerRow> rules = Arrays.stream(WorkflowAssigneeRule.values())
                    .filter(rule -> userName == null || userName.isBlank() || rule.getLabel().contains(userName.trim()))
                    .map(rule -> new HandlerRow(rule.expression(), rule.name(), rule.getLabel(), "动态规则", null)).toList();
            long offset = Math.multiplyExact(pageNum - 1, pageSize);
            return Result.ok(new HandlerResult(new HandlerPage(rules.stream().skip(offset).limit(pageSize).toList(),
                    rules.size()), null));
        }
        List<Dept> depts = deptService.lambdaQuery().eq(Dept::getUsable, 1).list();
        Page<HandlerRow> rows;
        switch (handlerType) {
            case "角色" -> {
                Page<Role> page = roleService.lambdaQuery().eq(Role::getUsable, 1)
                        .like(userName != null && !userName.isBlank(), Role::getRoleName, userName)
                        .page(new Page<>(pageNum, pageSize));
                rows = convert(page, page.getRecords().stream()
                        .map(role -> row("role", role.getId(), role.getRoleCode(), role.getRoleName(), "角色", role.getCreateTime()))
                        .toList());
            }
            case "部门" -> {
                Page<Dept> page = deptService.lambdaQuery().eq(Dept::getUsable, 1)
                        .like(userName != null && !userName.isBlank(), Dept::getDeptName, userName)
                        .page(new Page<>(pageNum, pageSize));
                rows = convert(page, page.getRecords().stream()
                        .map(dept -> row("dept", dept.getId(), dept.getDeptCode(), dept.getDeptName(), "部门", dept.getCreateTime()))
                        .toList());
            }
            case "用户" -> {
                Page<User> page = userService.lambdaQuery().eq(User::getUsable, 1)
                        .eq(groupId != null, User::getDeptId, groupId)
                        .and(userName != null && !userName.isBlank(), query -> query
                                .like(User::getName, userName).or().like(User::getAccount, userName))
                        .page(new Page<>(pageNum, pageSize));
                rows = convert(page, page.getRecords().stream()
                        .map(user -> row("user", user.getId(), user.getAccount(), user.getName(), "用户", user.getCreateTime()))
                        .toList());
            }
            default -> throw new IllegalArgumentException("不支持的办理人类型");
        }
        List<DeptTree> tree = depts.stream().filter(dept -> dept.getParentId() == null
                        || depts.stream().noneMatch(parent -> Objects.equals(parent.getId(), dept.getParentId())))
                .map(dept -> toTree(dept, depts)).toList();
        return Result.ok(new HandlerResult(new HandlerPage(rows.getRecords(), rows.getTotal()), tree));
    }

    @GetMapping("/handler-feedback")
    @SaCheckPermission("flow:definition:list")
    @Operation(summary = "回显已配置的办理人名称")
    public Result<List<HandlerFeedback>> handlerFeedback(@RequestParam(required = false) String storageIds) {
        if (storageIds == null || storageIds.isBlank()) {
            return Result.ok(List.of());
        }
        Long tenantId = RequestContextHolder.getTenantId();
        List<HandlerFeedback> result = new ArrayList<>();
        for (String raw : storageIds.split("[,，@]+")) {
            WorkflowAssigneeRule rule = WorkflowAssigneeRule.fromExpression(raw.trim());
            if (rule != null) {
                result.add(new HandlerFeedback(rule.expression(), rule.getLabel()));
                continue;
            }
            String[] parts = raw.trim().split(":", 2);
            if (parts.length != 2 || !parts[1].matches("[0-9]{1,19}")) {
                continue;
            }
            Long id;
            try {
                id = Long.valueOf(parts[1]);
            } catch (NumberFormatException ignored) {
                continue;
            }
            String name = switch (parts[0]) {
                case "role" -> {
                    Role role = roleService.getById(id);
                    yield role != null && Objects.equals(tenantId, role.getTenantId()) ? role.getRoleName() : null;
                }
                case "dept" -> {
                    Dept dept = deptService.getById(id);
                    yield dept != null && Objects.equals(tenantId, dept.getTenantId()) ? dept.getDeptName() : null;
                }
                case "user" -> {
                    User user = userService.getById(id);
                    yield user != null && Objects.equals(tenantId, user.getTenantId()) ? user.getName() : null;
                }
                default -> null;
            };
            if (name != null) {
                result.add(new HandlerFeedback(raw, name));
            }
        }
        return Result.ok(result);
    }

    private static HandlerRow row(String type, Long id, String code, String name, String group, LocalDateTime createTime) {
        return new HandlerRow(type + ":" + id, code, name, group, createTime);
    }

    private static <T> Page<HandlerRow> convert(Page<T> source, List<HandlerRow> records) {
        Page<HandlerRow> page = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        page.setRecords(records);
        return page;
    }

    private static DeptTree toTree(Dept dept, List<Dept> all) {
        return new DeptTree(dept.getId().toString(), dept.getDeptName(),
                dept.getParentId() == null ? "0" : dept.getParentId().toString(),
                all.stream().filter(child -> Objects.equals(child.getParentId(), dept.getId()))
                        .map(child -> toTree(child, all)).toList());
    }

    /** 办理人类型选项。 */
    public record HandlerDict(String label, String value, List<?> childList) { }

    /** 官方设计器办理人列表行。 */
    public record HandlerRow(String storageId, String handlerCode, String handlerName,
                             String groupName,
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime) { }

    /** 分页列表。 */
    public record HandlerPage(List<HandlerRow> rows, long total) { }

    /** 选择器的部门树节点。 */
    public record DeptTree(String id, String name, String parentId, List<DeptTree> children) { }

    /** 办理人列表及组织树。 */
    public record HandlerResult(HandlerPage handlerAuths, List<DeptTree> treeSelections) { }

    /** 已选办理人的名称回显。 */
    public record HandlerFeedback(String storageId, String handlerName) { }
}
