package io.github.yilers.flow.i18n;

import io.github.yilers.web.i18n.MessageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 工作流和请假示例的业务提示。 */
@Getter
@RequiredArgsConstructor
public enum FlowMessage implements MessageCode {

    // 通用
    TENANT_REQUIRED("flow.tenant-required", "Flow租户上下文不能为空"),
    PAGE_INVALID("flow.page-invalid", "页码必须大于0，且每页最多100条"),
    REJECT_REASON_REQUIRED("flow.reject-reason-required", "请填写退回原因"),

    // 请假申请
    LEAVE_START_NODE("flow.leave-start-node", "请假流程的申请提交节点必须是开始后的首个办理节点，且不能与其他任务并行"),
    LEAVE_TRANSACTION("flow.leave-transaction", "请假修改和重新提交必须在READ_COMMITTED事务中执行，请检查外层事务配置"),
    LEAVE_SINGLE_SUBMISSION("flow.leave-single-submission", "请假流程只能有一个仅使用动态规则“申请人”的申请提交节点"),
    LEAVE_FLOW_UNAVAILABLE("flow.leave-flow-unavailable", "请假流程不存在或无权操作"),
    LEAVE_TASK_CHANGED("flow.leave-task-changed", "申请任务已变更或无权操作，请刷新后重新打开请假表单"),
    LEAVE_CODE_REQUIRED("flow.leave-code-required", "请先配置请假业务流程编码"),
    LEAVE_SAVE_FAILED("flow.leave-save-failed", "保存请假草稿失败"),
    LEAVE_NOT_FOUND("flow.leave-not-found", "请假申请不存在或无权查看"),
    LEAVE_TYPE_INVALID("flow.leave-type-invalid", "请选择有效的请假类型"),
    LEAVE_TIME_INVALID("flow.leave-time-invalid", "请假结束时间必须晚于开始时间"),
    LEAVE_DURATION_INVALID("flow.leave-duration-invalid", "请假时长须为0.5小时的整数倍，且不超过8760小时"),
    LEAVE_DURATION_EXCEEDS("flow.leave-duration-exceeds", "请假时长不能超过起止时间的自然时长"),
    LEAVE_REASON_INVALID("flow.leave-reason-invalid", "请填写请假原因，最多1000字"),
    LEAVE_ID_INVALID("flow.leave-id-invalid", "请假申请ID必须为正数"),
    LEAVE_VERSION_REQUIRED("flow.leave-version-required", "请携带有效的申请版本号"),
    LEAVE_EDIT_CONFLICT("flow.leave-edit-conflict", "申请已变更、当前不可编辑或无权操作，请刷新后重新打开表单"),

    // 待办、抄送与流程详情
    CC_ID_INVALID("flow.cc-id-invalid", "抄送记录ID必须为正数"),
    CC_NOT_FOUND("flow.cc-not-found", "抄送记录不存在或无权查看"),
    INSTANCE_ID_INVALID("flow.instance-id-invalid", "流程实例ID必须为正数"),
    INSTANCE_NOT_FOUND("flow.instance-not-found", "流程不存在或无权查看"),
    INSTANCE_MULTIPLE_BUSINESS("flow.instance-multiple-business", "流程实例关联了多个业务单据，请检查业务配置"),
    TASK_IDENTITY_INVALID("flow.task-identity-invalid", "当前用户没有有效的流程办理身份"),

    // 审批和流转
    TARGET_ID_INVALID("flow.target-id-invalid", "目标用户ID必须为正数"),
    TARGET_SELF("flow.target-self", "不能将任务转给自己"),
    TARGET_UNAVAILABLE("flow.target-unavailable", "目标用户不存在、已停用或不属于当前租户"),
    TAKEBACK_NO_TASK("flow.takeback-no-task", "流程已没有待办任务，不能拿回"),
    REVOKE_NO_TASK("flow.revoke-no-task", "流程已没有待办任务，不能撤销"),
    ACTION_TRANSACTION("flow.action-transaction", "流程操作必须在READ_COMMITTED事务中执行，请检查外层事务配置"),
    INSTANCE_CHANGED("flow.instance-changed", "流程实例不存在、已变化或无权操作"),
    INSTANCE_STATUS_INVALID("flow.instance-status-invalid", "流程当前状态不支持此操作"),
    SUBMISSION_WRONG_BUSINESS("flow.submission-wrong-business", "该任务不是指定业务的申请提交任务"),
    SUBMISSION_NO_APPROVAL("flow.submission-no-approval", "这是申请提交任务，请从业务表单修改或重新提交，不能使用审批通过或退回"),
    APPROVAL_TRANSACTION("flow.approval-transaction", "流程审批必须在READ_COMMITTED事务中执行，请检查业务外层事务配置"),
    TASK_CHANGED("flow.task-changed", "任务已处理、不存在或无权办理，请刷新待办"),
    DELEGATE_FAILED("flow.delegate-failed", "委派失败，请刷新待办后重试"),
    TRANSFER_FAILED("flow.transfer-failed", "转办失败，请刷新待办后重试"),
    DELEGATE_DISABLED("flow.delegate-disabled", "当前审批节点未允许委派"),
    TRANSFER_DISABLED("flow.transfer-disabled", "当前审批节点未允许转办"),
    NODE_ASSIGNEE_MISSING("flow.node-assignee-missing", "节点“{0}”未解析出有效审批人，请检查办理人配置"),

    // 请求参数校验
    COMMENT_TOO_LONG("flow.comment-too-long", "审批意见最多500个字符"),
    TARGET_REQUIRED("flow.target-required", "请选择目标用户"),
    ACTION_NOTE_TOO_LONG("flow.action-note-too-long", "操作说明最多500个字符"),

    // 流程定义
    DEFINITION_PAGE_INVALID("flow.definition-page-invalid", "页码必须大于0，且每页最多200条"),
    DEFINITION_NEW_ID("flow.definition-new-id", "新建流程定义不能指定ID"),
    DEFINITION_IMMUTABLE("flow.definition-immutable", "流程定义ID、编码和版本不能在编辑时变更"),
    DEFINITION_SAVE_FAILED("flow.definition-save-failed", "保存流程定义失败：{0}"),
    DEFINITION_COPY_FAILED("flow.definition-copy-failed", "复制流程定义失败"),
    DEFINITION_DELETE_FAILED("flow.definition-delete-failed", "删除流程定义失败"),
    DEFINITION_NOT_FOUND("flow.definition-not-found", "流程定义不存在"),
    DEFINITION_EXPIRED("flow.definition-expired", "该流程定义版本已失效，请复制新版本后修改"),
    DEFINITION_IN_USE("flow.definition-in-use", "该版本已有流程实例，不能修改或删除"),
    DEFINITION_UNPUBLISH_FIRST("flow.definition-unpublish-first", "请先取消发布该流程定义版本"),
    DEFINITION_CC_APPROVAL_ONLY("flow.definition-cc-approval-only", "只有审批节点可以配置自动抄送"),
    DEFINITION_JSON_INVALID("flow.definition-json-invalid", "流程定义JSON格式不正确"),
    DEFINITION_JSON_REQUIRED("flow.definition-json-required", "流程定义JSON不能为空"),

    // 动态办理人
    IDENTITY_APPLICANT_ONLY("flow.identity-applicant-only", "动态办理人只能依据当前申请人的组织信息生成"),
    IDENTITY_APPLICANT_MISSING("flow.identity-applicant-missing", "申请人不存在或已停用"),
    IDENTITY_DEPT_REQUIRED("flow.identity-dept-required", "申请人没有有效的所属部门"),
    IDENTITY_DEPT_MISSING("flow.identity-dept-missing", "申请人所在部门不存在或已停用"),
    IDENTITY_PARENT_REQUIRED("flow.identity-parent-required", "申请人所在部门没有有效的上一级部门"),
    IDENTITY_PARENT_LEADER_MISSING("flow.identity-parent-leader-missing", "上一级部门未配置有效主管，或主管就是申请人本人"),
    IDENTITY_DEPT_LEADER_MISSING("flow.identity-dept-leader-missing", "本部门未配置有效副主管或主管，或审批人就是申请人本人"),
    IDENTITY_USER_INVALID("flow.identity-user-invalid", "当前用户没有有效的流程身份"),
    ASSIGNEE_FLOW_NOT_PUBLISHED("flow.assignee-flow-not-published", "当前租户没有已发布的流程：{0}"),
    ASSIGNEE_RULE_EMPTY("flow.assignee-rule-empty", "“{0}”未找到有效审批人，请检查部门主管配置"),
    ASSIGNEE_VARIABLE_RESERVED("flow.assignee-variable-reserved", "不允许提交内置办理人变量：{0}"),

    // 节点抄送配置
    CC_CONFIG_INVALID("flow.cc-config-invalid", "节点抄送配置格式不正确：{0}"),
    CC_RECIPIENT_COUNT("flow.cc-recipient-count", "启用抄送后请选择1至100个抄送对象"),
    CC_RECIPIENT_DUPLICATE("flow.cc-recipient-duplicate", "抄送对象不能重复"),
    CC_RECIPIENT_UNSUPPORTED("flow.cc-recipient-unsupported", "不支持的抄送对象：{0}"),
    CC_RECIPIENT_ID_RANGE("flow.cc-recipient-id-range", "抄送对象ID超出范围：{0}"),
    BUSINESS_MULTIPLE_ENTRIES("flow.business-multiple-entries", "任务关联了多个业务提交入口，请检查配置"),

    // 原有参数异常兼容提示
    ARGUMENT_TYPE_UNSUPPORTED("flow.argument-type-unsupported", "不支持的办理人类型"),
    ARGUMENT_USER_PAGE_INVALID("flow.argument-user-page-invalid", "用户分页参数不合法"),
    ARGUMENT_ASSIGNEE_PAGE_INVALID("flow.argument-assignee-page-invalid", "办理人分页参数不合法"),
    ARGUMENT_IDENTITY_TYPE_UNSUPPORTED("flow.argument-identity-type-unsupported", "不支持的流程办理人类型"),
    ARGUMENT_ROLE_UNAVAILABLE("flow.argument-role-unavailable", "流程办理角色不存在或已停用"),
    ARGUMENT_DEPT_UNAVAILABLE("flow.argument-dept-unavailable", "流程办理部门不存在或已停用"),
    ARGUMENT_FLAG_REQUIRED("flow.argument-flag-required", "流程办理人标识不能为空"),
    ARGUMENT_FLAG_INVALID("flow.argument-flag-invalid", "流程办理人标识不合法"),
    ARGUMENT_DEFINITION_ID("flow.argument-definition-id", "流程定义ID必须为正数"),
    ARGUMENT_TASK_ID("flow.argument-task-id", "任务ID必须为正数"),
    ARGUMENT_FLOW_CODE("flow.argument-flow-code", "流程编码不能为空"),
    ARGUMENT_BUSINESS_ID("flow.argument-business-id", "业务单据ID不能为空"),
    ARGUMENT_BUSINESS_TYPE("flow.argument-business-type", "业务类型不能为空"),
    ARGUMENT_FLOW_NAME("flow.argument-flow-name", "流程名称不能为空"),
    ARGUMENT_DEFINITION_JSON("flow.argument-definition-json", "流程定义JSON不能为空"),

    // 动态办理人规则名称
    RULE_APPLICANT("flow.rule-applicant", "申请人"),
    RULE_DEPARTMENT_APPROVERS("flow.rule-department-approvers", "本部门副主管（无则主管）"),
    RULE_PARENT_APPROVERS("flow.rule-parent-approvers", "上一级部门主管");

    private final String key;
    private final String defaultMessage;

    @Override
    public String getBasename() {
        return "i18n/flow-messages";
    }
}
