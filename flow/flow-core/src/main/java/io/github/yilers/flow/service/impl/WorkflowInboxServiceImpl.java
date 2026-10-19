package io.github.yilers.flow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.flow.mapper.WorkflowInboxMapper;
import io.github.yilers.flow.model.WorkflowInstanceDetail;
import io.github.yilers.flow.model.WorkflowPage;
import io.github.yilers.flow.model.WorkflowWorkItem;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.service.WorkflowInboxService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
        WorkflowWorkItem instance = mapper.visibleInstance(instanceId, tenantId, userId, flags);
        if (instance == null) {
            // 未授权与不存在使用相同响应，避免通过实例ID探测其他人的流程。
            throw new CommonException(404, "流程不存在或无权查看");
        }
        return new WorkflowInstanceDetail(instance, mapper.tasks(instanceId, tenantId, userId, flags),
                mapper.history(instanceId, tenantId), sanitizeChart(mapper.chart(instanceId, tenantId)));
    }

    /** 流程图只保留展示数据，不把运行变量、监听器配置和业务扩展正文交给浏览器。 */
    String sanitizeChart(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Map<String, Object> chart = FlowEngine.jsonConvert.strToMap(json);
        stripPrivateFields(chart);
        return FlowEngine.jsonConvert.objToStr(chart);
    }

    private void stripPrivateFields(Object value) {
        if (value instanceof Map<?, ?> map) {
            map.keySet().removeIf(PRIVATE_CHART_FIELDS::contains);
            map.values().forEach(this::stripPrivateFields);
        } else if (value instanceof List<?> list) {
            list.forEach(this::stripPrivateFields);
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
