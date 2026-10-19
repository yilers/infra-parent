package io.github.yilers.flow.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/** 内置动态办理人规则。表达式使用Warm-Flow原生变量机制，不额外实现流程引擎。 */
@Getter
@RequiredArgsConstructor
public enum WorkflowAssigneeRule {

    APPLICANT("wfApplicant", "申请人"),
    DEPARTMENT_APPROVERS("wfDepartmentApprovers", "本部门副主管（无则主管）"),
    PARENT_DEPARTMENT_APPROVERS("wfParentDepartmentApprovers", "上一级部门主管");

    private final String variable;
    private final String label;

    public String expression() {
        return "${" + variable + "}";
    }

    public static WorkflowAssigneeRule fromExpression(String expression) {
        return Arrays.stream(values()).filter(rule -> rule.expression().equals(expression)).findFirst().orElse(null);
    }
}
