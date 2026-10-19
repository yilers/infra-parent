package io.github.yilers.flow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.flow.mapper.WorkflowInboxMapper;
import io.github.yilers.flow.model.WorkflowInstanceDetail;
import io.github.yilers.flow.model.WorkflowPage;
import io.github.yilers.flow.model.WorkflowWorkItem;
import io.github.yilers.flow.model.WorkflowBusinessReference;
import io.github.yilers.flow.service.WorkflowBusinessProvider;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.service.WorkflowInboxService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    @Transactional(readOnly = true)
    public WorkflowInstanceDetail detail(Long instanceId) {
        if (instanceId == null || instanceId <= 0) {
            throw new CommonException("流程实例ID必须为正数");
        }
        String userId = identityProvider.currentUserId();
        String tenantId = tenantId();
        List<String> flags = flags();
        WorkflowWorkItem instance = requireVisible(instanceId, tenantId, userId, flags);
        return new WorkflowInstanceDetail(instance, mapper.tasks(instanceId, tenantId, userId, flags),
                mapper.history(instanceId, tenantId), sanitizeChart(mapper.chart(instanceId, tenantId)), business(instanceId));
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

    /** 流程图只保留展示数据，不把运行变量、监听器配置和业务扩展正文交给浏览器。 */
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
            }
            map.keySet().removeIf(PRIVATE_CHART_FIELDS::contains);
            map.values().forEach(item -> stripPrivateFields(item, names));
        } else if (value instanceof List<?> list) {
            list.forEach(item -> stripPrivateFields(item, names));
        }
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
        return new WorkflowPage(page.getTotal(), page.getRecords());
    }

    private String normalize(String flowCode) {
        return flowCode == null || flowCode.isBlank() ? null : flowCode.trim();
    }
}
