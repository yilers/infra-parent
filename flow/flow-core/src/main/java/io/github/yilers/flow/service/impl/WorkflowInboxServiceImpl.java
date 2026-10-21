package io.github.yilers.flow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.flow.mapper.WorkflowInboxMapper;
import io.github.yilers.flow.mapper.WorkflowCcMapper;
import io.github.yilers.flow.entity.WorkflowCc;
import io.github.yilers.flow.model.WorkflowBusinessReference;
import io.github.yilers.flow.model.WorkflowInstanceDetail;
import io.github.yilers.flow.model.WorkflowPage;
import io.github.yilers.flow.model.WorkflowWorkItem;
import io.github.yilers.flow.model.WorkflowUserInfo;
import io.github.yilers.flow.service.WorkflowBusinessProvider;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.service.WorkflowInboxService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用审批中心查询。所有查询先确定可信身份，详情先授权再加载历史与流程图。
 */
@Service
@RequiredArgsConstructor
public class WorkflowInboxServiceImpl implements WorkflowInboxService {

    private static final Set<String> PRIVATE_CHART_FIELDS = Set.of("variable", "ext", "listenerType",
            "listenerPath", "permissionFlag", "permissionList", "formPath");

    private final WorkflowIdentityProvider identityProvider;
    private final WorkflowInboxMapper mapper;
    private final WorkflowCcMapper ccMapper;
    private final List<WorkflowBusinessProvider> businessProviders;

    @Override
    public WorkflowPage todo(String flowCode, int pageNum, int pageSize) {
        Page<WorkflowWorkItem> page = page(pageNum, pageSize);
        String userId = identityProvider.currentUserId();
        return result(mapper.todo(page, tenantId(), userId, flags(), normalize(flowCode)));
    }

    @Override
    public WorkflowPage done(String flowCode, int pageNum, int pageSize) {
        Page<WorkflowWorkItem> page = page(pageNum, pageSize);
        String userId = identityProvider.currentUserId();
        return result(mapper.done(page, tenantId(), userId, normalize(flowCode)));
    }

    @Override
    public WorkflowPage started(String flowCode, int pageNum, int pageSize) {
        Page<WorkflowWorkItem> page = page(pageNum, pageSize);
        String userId = identityProvider.currentUserId();
        return result(mapper.started(page, tenantId(), userId, normalize(flowCode)));
    }

    @Override
    public WorkflowPage cc(String flowCode, int pageNum, int pageSize) {
        return result(mapper.cc(page(pageNum, pageSize), tenantId(),
                identityProvider.currentUserId(), normalize(flowCode)));
    }

    @Override
    @Transactional
    public void readCc(Long ccId) {
        if (ccId == null || ccId <= 0) throw new CommonException("抄送记录ID必须为正数");
        String userId = identityProvider.currentUserId();
        String tenantId = tenantId();
        WorkflowCc cc = ccMapper.selectById(ccId);
        if (cc == null || !tenantId.equals(cc.getTenantId()) || !userId.equals(cc.getRecipientId())) {
            throw new CommonException(404, "抄送记录不存在或无权查看");
        }
        if (cc.getReadTime() == null) {
            ccMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<WorkflowCc>()
                    .eq(WorkflowCc::getId, ccId).eq(WorkflowCc::getTenantId, tenantId)
                    .eq(WorkflowCc::getRecipientId, userId).isNull(WorkflowCc::getReadTime)
                    .set(WorkflowCc::getReadTime, new java.util.Date()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowInstanceDetail detail(Long instanceId) {
        if (instanceId == null || instanceId <= 0) {
            throw new CommonException("流程实例ID必须为正数");
        }
        String userId = identityProvider.currentUserId();
        String tenantId = tenantId();
        List<String> flags = flags();
        WorkflowWorkItem instance = requireVisible(instanceId, tenantId, userId, flags);
        List<WorkflowWorkItem> tasks = mapper.tasks(instanceId, tenantId, userId, flags);
        List<WorkflowWorkItem> history = mapper.history(instanceId, tenantId);
        List<WorkflowWorkItem> taskRecords = new ArrayList<>(tasks);
        taskRecords.addAll(history);
        taskRecords.add(instance);
        decorateTasks(taskRecords);
        decorateTaskActions(tasks, mapper.chart(instanceId, tenantId));
        return new WorkflowInstanceDetail(instance, tasks, history,
                sanitizeChart(mapper.chart(instanceId, tenantId)), business(instanceId));
    }

    @Override
    public void checkInstanceAccess(Long instanceId) {
        if (instanceId == null || instanceId <= 0) {
            throw new CommonException("流程实例ID必须为正数");
        }
        String userId = identityProvider.currentUserId();
        requireVisible(instanceId, tenantId(), userId, flags());
    }

    private WorkflowWorkItem requireVisible(Long instanceId, String tenantId, String userId, List<String> flags) {
        WorkflowWorkItem instance = mapper.visibleInstance(instanceId, tenantId, userId, flags);
        if (instance == null) {
            throw new CommonException(404, "流程不存在或无权查看");
        }
        return instance;
    }

    private WorkflowBusinessReference business(Long instanceId) {
        List<WorkflowBusinessReference> references = businessProviders.stream()
                .map(provider -> provider.findReference(instanceId)).filter(java.util.Objects::nonNull).toList();
        if (references.size() > 1) {
            throw new CommonException("流程实例关联了多个业务单据，请检查业务配置");
        }
        return references.isEmpty() ? null : references.getFirst();
    }

    /** 流程图只返回展示摘要；运行变量、监听器类路径和业务扩展正文不会交给浏览器。 */
    String sanitizeChart(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Map<String, Object> chart = FlowEngine.jsonConvert.strToMap(json);
        Set<String> flags = new LinkedHashSet<>();
        collectChartFlags(chart, flags);
        Map<String, String> names = flags.isEmpty() ? Map.of() : identityProvider.resolvePermissionNames(List.copyOf(flags));
        stripPrivateFields(chart, names);
        return FlowEngine.jsonConvert.objToStr(chart);
    }

    /** 先汇总去重，避免同一办理人在多个节点重复查询；原始标识随后仍会过滤。 */
    private void collectChartFlags(Object value, Set<String> flags) {
        if (value instanceof Map<?, ?> map) {
            Object flag = map.get("permissionFlag");
            if (flag instanceof String text && !text.isBlank()) {
                Arrays.stream(text.split("@@")).map(String::trim).filter(item -> !item.isEmpty()).forEach(flags::add);
            }
            WorkflowCcConfig ccConfig = chartCcConfig(map.get("ext"));
            if (!ccConfig.recipients().isEmpty()) flags.addAll(ccConfig.recipients());
            map.values().forEach(item -> collectChartFlags(item, flags));
        } else if (value instanceof List<?> list) {
            list.forEach(item -> collectChartFlags(item, flags));
        }
    }

    private void stripPrivateFields(Object value, Map<String, String> names) {
        if (value instanceof Map<?, ?> map) {
            if (map.containsKey("nodeType")) {
                // 展示名称不是办理权限。空配置或已失效标识不能被前端误显示成“所有人”。
                Object flag = map.get("permissionFlag");
                String label = flag instanceof String text && !text.isBlank()
                        ? Arrays.stream(text.split("@@")).map(String::trim).filter(item -> !item.isEmpty())
                            .map(item -> names.getOrDefault(item, "指定审批人（无法解析）")).distinct().collect(Collectors.joining("、"))
                        : "未指定审批人";
                @SuppressWarnings("unchecked")
                Map<String, Object> node = (Map<String, Object>) map;
                node.put("permissionLabel", label);
                node.put("listenerDisplayRows", listenerDisplayRows(map.get("listenerType"), map.get("listenerPath")));
                node.put("flowCcDisplay", ccDisplay(chartCcConfig(map.get("ext")), names));
                node.put("flowTaskActionsDisplay", taskActionsDisplay(map.get("ext")));
            }
            map.keySet().removeIf(PRIVATE_CHART_FIELDS::contains);
            map.values().forEach(item -> stripPrivateFields(item, names));
        } else if (value instanceof List<?> list) {
            list.forEach(item -> stripPrivateFields(item, names));
        }
    }

    /** 为实例详情构造只读配置摘要，不返回监听器实现类路径或完整的节点扩展属性。 */
    private WorkflowCcConfig chartCcConfig(Object ext) {
        if (ext == null) return new WorkflowCcConfig(false, false, List.of());
        try {
            String json = ext instanceof String text ? text : FlowEngine.jsonConvert.objToStr(ext);
            return WorkflowCcConfig.fromExt(json);
        } catch (RuntimeException ignored) {
            // 历史流程图可能包含旧版或损坏的扩展值；查看详情不应因此失败。
            return new WorkflowCcConfig(false, false, List.of());
        }
    }

    private List<Map<String, Object>> listenerDisplayRows(Object rawTypes, Object rawPaths) {
        if (!(rawTypes instanceof String types) || types.isBlank()) return List.of();
        String[] typeRows = types.split(",", -1);
        String[] pathRows = rawPaths instanceof String paths ? paths.split("@@", -1) : new String[0];
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < typeRows.length; index++) {
            String type = typeRows[index].trim();
            if (type.isEmpty()) continue;
            boolean configured = index < pathRows.length && !pathRows[index].isBlank();
            rows.add(Map.of("type", type, "configured", configured));
        }
        return rows;
    }

    private Map<String, Object> ccDisplay(WorkflowCcConfig config, Map<String, String> names) {
        List<Map<String, String>> recipients = config.recipients().stream().map(flag -> {
            String kind = flag.startsWith("user:") ? "用户" : flag.startsWith("role:") ? "角色"
                    : flag.startsWith("dept:") ? "部门" : "动态规则";
            return Map.of("kind", kind, "name", names.getOrDefault(flag, "对象已失效"));
        }).toList();
        return Map.of("enabled", config.enabled(), "includeChildDepartments", config.includeChildDepartments(),
                "recipients", recipients);
    }

    /** 仅向流程参与人展示节点允许的操作，不暴露原始扩展配置。 */
    private Map<String, Boolean> taskActionsDisplay(Object rawExt) {
        String ext = rawExt instanceof String text ? text : FlowEngine.jsonConvert.objToStr(rawExt);
        WorkflowTaskActionConfig config = WorkflowTaskActionConfig.fromExt(ext);
        return Map.of("transferEnabled", config.transferEnabled(),
                "delegationEnabled", config.delegationEnabled());
    }

    private List<String> flags() {
        List<String> flags = identityProvider.currentPermissionFlags();
        if (flags == null || flags.isEmpty()) {
            throw new CommonException("当前用户没有有效的流程办理身份");
        }
        List<String> result = flags.stream().filter(flag -> flag != null && !flag.isBlank()).distinct().toList();
        if (result.isEmpty()) {
            throw new CommonException("当前用户没有有效的流程办理身份");
        }
        return result;
    }

    private String tenantId() {
        Long tenantId = RequestContextHolder.getTenantId();
        if (tenantId == null) {
            throw new CommonException("Flow租户上下文不能为空");
        }
        return tenantId.toString();
    }

    private Page<WorkflowWorkItem> page(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new CommonException("页码必须大于0，且每页最多100条");
        }
        return new Page<>(pageNum, pageSize);
    }

    private WorkflowPage result(Page<WorkflowWorkItem> page) {
        decorateTasks(page.getRecords());
        return new WorkflowPage(page.getTotal(), page.getRecords());
    }

    private void decorateTasks(List<WorkflowWorkItem> items) {
        // 退回多次会有同一节点的多条历史，单次查询内复用关联，避免逐条重复查询业务和定义。
        Map<TaskKey, Optional<WorkflowBusinessReference>> references = new HashMap<>();
        items.forEach(item -> {
            if (item.getTaskId() != null && item.getNodeCode() != null) {
                TaskKey key = new TaskKey(item.getInstanceId(), item.getNodeCode());
                item.setSubmission(references.computeIfAbsent(key, value -> Optional.ofNullable(
                        WorkflowBusinessTasks.submission(businessProviders, value.instanceId(), value.nodeCode()))).orElse(null));
            }
        });
        Set<String> userIds = new LinkedHashSet<>();
        items.forEach(item -> {
            if (item.getInitiatorId() != null && !item.getInitiatorId().isBlank()) userIds.add(item.getInitiatorId());
            if (item.getApproverId() != null && !item.getApproverId().isBlank()) userIds.add(item.getApproverId());
        });
        if (userIds.isEmpty()) return;
        Map<String, WorkflowUserInfo> users = identityProvider.resolveUsers(List.copyOf(userIds));
        items.forEach(item -> {
            WorkflowUserInfo initiator = users.get(item.getInitiatorId());
            WorkflowUserInfo approver = users.get(item.getApproverId());
            if (initiator != null) item.setInitiatorName(initiator.name());
            if (approver != null) item.setApproverName(approver.name());
        });
    }

    /** 当前任务操作能力只从实例定义快照读取；权限码由前端角色权限和后端接口共同校验。 */
    private void decorateTaskActions(List<WorkflowWorkItem> tasks, String chart) {
        tasks.forEach(task -> {
            WorkflowTaskActionConfig config = WorkflowTaskActionConfig.fromChart(chart, task.getNodeCode());
            task.setCanTransfer(config.transferEnabled());
            task.setCanDelegate(config.delegationEnabled());
        });
    }

    private record TaskKey(Long instanceId, String nodeCode) { }

    private String normalize(String flowCode) {
        return flowCode == null || flowCode.isBlank() ? null : flowCode.trim();
    }
}
