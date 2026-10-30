package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.i18n.FlowMessage;
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
            throw new CommonException(FlowMessage.ASSIGNEE_FLOW_NOT_PUBLISHED, flowCode);
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
                FlowMessage ruleName = switch (rule) {
                    case APPLICANT -> FlowMessage.RULE_APPLICANT;
                    case DEPARTMENT_APPROVERS -> FlowMessage.RULE_DEPARTMENT_APPROVERS;
                    case PARENT_DEPARTMENT_APPROVERS -> FlowMessage.RULE_PARENT_APPROVERS;
                };
                throw new CommonException(FlowMessage.ASSIGNEE_RULE_EMPTY, ruleName);
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
                throw new CommonException(FlowMessage.ASSIGNEE_VARIABLE_RESERVED, rule.getVariable());
            }
        }
        return result;
    }
}
