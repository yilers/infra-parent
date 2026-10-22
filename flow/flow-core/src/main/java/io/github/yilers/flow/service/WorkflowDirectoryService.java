package io.github.yilers.flow.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.model.WorkflowDepartmentInfo;
import io.github.yilers.flow.model.WorkflowDirectoryEntry;
import io.github.yilers.flow.model.WorkflowDirectoryPage;
import io.github.yilers.flow.model.WorkflowDirectoryType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Flow 设计器与任务操作所需的目录查询和展示组装。
 * <p>
 * 组织数据通过 {@link WorkflowDataProvider} 获取，规则标签及已选人员名称由 Flow 统一解释；
 * 本服务不依赖 UPM 实体、Service 或 MyBatis-Plus 分页对象。
 */
@Service
public class WorkflowDirectoryService {

    private static final List<HandlerDict> HANDLER_TYPES = List.of(
            new HandlerDict("角色", "role", List.of()),
            new HandlerDict("部门", "dept", List.of()),
            new HandlerDict("用户", "user", List.of()),
            new HandlerDict("动态规则", "dynamic", List.of()));

    private final WorkflowDataProvider dataProvider;
    private final WorkflowIdentityProvider identityProvider;

    public WorkflowDirectoryService(WorkflowDataProvider dataProvider, WorkflowIdentityProvider identityProvider) {
        this.dataProvider = dataProvider;
        this.identityProvider = identityProvider;
    }

    public List<String> handlerTypes() {
        return List.of("角色", "部门", "用户", "动态规则");
    }

    public List<HandlerDict> handlerDictionary() {
        return HANDLER_TYPES;
    }

    public HandlerResult searchHandlers(String handlerType, String keyword, Long departmentId,
                                        long pageNum, long pageSize) {
        validatePage(pageNum, pageSize, "办理人");
        String normalizedType = handlerType == null || handlerType.isBlank() ? "角色" : handlerType;
        String normalizedKeyword = normalizeKeyword(keyword);
        if ("动态规则".equals(normalizedType)) {
            List<HandlerRow> rules = java.util.Arrays.stream(WorkflowAssigneeRule.values())
                    .filter(rule -> normalizedKeyword == null || rule.getLabel().contains(normalizedKeyword))
                    .map(rule -> new HandlerRow(rule.expression(), rule.name(), rule.getLabel(), "动态规则", null))
                    .toList();
            long offset = Math.multiplyExact(pageNum - 1, pageSize);
            return new HandlerResult(new HandlerPage(rules.stream().skip(offset).limit(pageSize).toList(), rules.size()), null);
        }

        WorkflowDirectoryType directoryType = switch (normalizedType) {
            case "角色" -> WorkflowDirectoryType.ROLE;
            case "部门" -> WorkflowDirectoryType.DEPARTMENT;
            case "用户" -> WorkflowDirectoryType.USER;
            default -> throw new IllegalArgumentException("不支持的办理人类型");
        };
        WorkflowDirectoryPage page = dataProvider.searchDirectory(directoryType, normalizedKeyword,
                departmentId == null ? null : departmentId.toString(), pageNum, pageSize);
        List<HandlerRow> rows = page.records().stream().map(WorkflowDirectoryService::toHandlerRow).toList();
        return new HandlerResult(new HandlerPage(rows, page.total()),
                buildDepartmentTree(dataProvider.findAllDepartments()));
    }

    /**
     * 批量回显已保存的静态办理人名称，避免按每个选择项逐个访问用户、角色或部门服务。
     */
    public List<HandlerFeedback> resolveHandlerFeedback(String storageIds) {
        if (storageIds == null || storageIds.isBlank()) return List.of();
        List<String> requested = List.of(storageIds.split("[,，@]+"));
        Set<String> flags = new LinkedHashSet<>();
        for (String raw : requested) {
            String flag = raw.trim();
            if (flag.isEmpty()) continue;
            if (WorkflowAssigneeRule.fromExpression(flag) != null) {
                flags.add(flag);
                continue;
            }
            String[] parts = flag.split(":", 2);
            if (parts.length != 2 || !Set.of("role", "dept", "user").contains(parts[0]) || !isPositiveId(parts[1])) {
                continue;
            }
            flags.add(flag);
        }
        if (flags.isEmpty()) return List.of();
        Map<String, String> names = identityProvider.resolvePermissionNames(List.copyOf(flags));
        List<HandlerFeedback> result = new ArrayList<>();
        for (String flag : flags) {
            String name = names.get(flag);
            if (name != null && !name.isBlank()) result.add(new HandlerFeedback(flag, name));
        }
        return result;
    }

    public WorkflowDirectoryPage searchTaskTargets(String keyword, long pageNum, long pageSize) {
        validatePage(pageNum, pageSize, "用户");
        return dataProvider.searchTransferTargets(normalizeKeyword(keyword), identityProvider.currentUserId(),
                pageNum, pageSize);
    }

    private static HandlerRow toHandlerRow(WorkflowDirectoryEntry entry) {
        String type = switch (entry.type()) {
            case ROLE -> "角色";
            case DEPARTMENT -> "部门";
            case USER -> "用户";
        };
        String prefix = switch (entry.type()) {
            case ROLE -> "role";
            case DEPARTMENT -> "dept";
            case USER -> "user";
        };
        return new HandlerRow(prefix + ":" + entry.id(), entry.code(), entry.name(), type, entry.createTime());
    }

    private static List<DeptTree> buildDepartmentTree(List<WorkflowDepartmentInfo> departments) {
        Map<String, List<WorkflowDepartmentInfo>> children = new LinkedHashMap<>();
        Set<String> ids = new LinkedHashSet<>();
        departments.forEach(dept -> ids.add(dept.id()));
        for (WorkflowDepartmentInfo department : departments) {
            String parentId = department.parentId();
            if (parentId != null && ids.contains(parentId)) {
                children.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(department);
            }
        }
        Set<String> visited = new LinkedHashSet<>();
        List<DeptTree> roots = departments.stream()
                .filter(dept -> dept.parentId() == null || !ids.contains(dept.parentId()))
                .map(dept -> toTree(dept, children, visited)).filter(Objects::nonNull).toList();
        // 防止异常循环层级导致部门目录整棵消失；未被正常根节点遍历到的节点作为根节点返回。
        List<DeptTree> result = new ArrayList<>(roots);
        departments.stream().filter(dept -> !visited.contains(dept.id()))
                .map(dept -> toTree(dept, children, visited)).filter(Objects::nonNull).forEach(result::add);
        return List.copyOf(result);
    }

    private static DeptTree toTree(WorkflowDepartmentInfo department,
                                   Map<String, List<WorkflowDepartmentInfo>> children, Set<String> visited) {
        if (!visited.add(department.id())) return null;
        List<DeptTree> childTrees = children.getOrDefault(department.id(), List.of()).stream()
                .map(child -> toTree(child, children, visited)).filter(Objects::nonNull).toList();
        return new DeptTree(department.id(), department.name(),
                department.parentId() == null ? "0" : department.parentId(), childTrees);
    }

    private static void validatePage(long pageNum, long pageSize, String subject) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException(subject + "分页参数不合法");
        }
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private static boolean isPositiveId(String id) {
        if (!id.matches("[0-9]{1,19}")) return false;
        try {
            return Long.parseLong(id) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** 办理人类型选项。 */
    public record HandlerDict(String label, String value, List<?> childList) { }

    /** 官方设计器办理人列表行。 */
    public record HandlerRow(String storageId, String handlerCode, String handlerName, String groupName,
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime) { }

    /** 办理人分页数据。 */
    public record HandlerPage(List<HandlerRow> rows, long total) { }

    /** 部门树节点。 */
    public record DeptTree(String id, String name, String parentId, List<DeptTree> children) { }

    /** 办理人列表及部门树。 */
    public record HandlerResult(HandlerPage handlerAuths, List<DeptTree> treeSelections) { }

    /** 已选办理人名称回显。 */
    public record HandlerFeedback(String storageId, String handlerName) { }

}
