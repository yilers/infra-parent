package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 仅在发起时准备定义中实际使用的内置规则，避免固定审批流程也必须配置部门主管。 */
@Component
@RequiredArgsConstructor
public class WorkflowAssigneeResolver {

    private final WorkflowIdentityProvider identityProvider;

    public Map<String, Object> prepare(String flowCode, String applicantId, Map<String, Object> variables) {
        Map<String, Object> result = copyBusinessVariables(variables);
        var definition = FlowEngine.defService().getPublishByFlowCode(flowCode);
        if (definition == null) {
            throw new CommonException("当前租户没有已发布的流程：" + flowCode);
        }
        Set<WorkflowAssigneeRule> rules = new LinkedHashSet<>();
        FlowEngine.nodeService().list(FlowEngine.newNode().setDefinitionId(definition.getId())).forEach(node -> {
            if (node.getPermissionFlag() != null) {
                Arrays.stream(node.getPermissionFlag().split("@@")).map(String::trim)
                        .map(WorkflowAssigneeRule::fromExpression).filter(java.util.Objects::nonNull).forEach(rules::add);
            }
        });
        for (WorkflowAssigneeRule rule : rules) {
            List<String> resolved = identityProvider.resolveDynamicAssignees(applicantId, rule);
            if (resolved == null || resolved.isEmpty() || resolved.stream().anyMatch(id -> id == null || id.isBlank())) {
                throw new CommonException("“" + rule.getLabel() + "”未找到有效审批人，请检查部门主管配置");
            }
            result.put(rule.getVariable(), resolved.stream().distinct().toList());
        }
        return result;
    }

    /** 内置规则变量只能由服务端准备，业务调用和浏览器不能在发起或办理时覆盖快照。 */
    public Map<String, Object> copyBusinessVariables(Map<String, Object> variables) {
        Map<String, Object> result = variables == null ? new HashMap<>() : new HashMap<>(variables);
        for (WorkflowAssigneeRule rule : WorkflowAssigneeRule.values()) {
            if (result.containsKey(rule.getVariable())) {
                throw new CommonException("不允许提交内置办理人变量：" + rule.getVariable());
            }
        }
        return result;
    }
}
