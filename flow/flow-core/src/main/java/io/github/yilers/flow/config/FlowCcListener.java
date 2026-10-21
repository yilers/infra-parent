package io.github.yilers.flow.config;

import io.github.yilers.flow.entity.WorkflowCc;
import io.github.yilers.flow.mapper.WorkflowCcMapper;
import io.github.yilers.flow.model.WorkflowUserInfo;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.flow.service.impl.WorkflowCcConfig;
import io.github.yilers.web.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 引擎完成节点并创建后续任务时调用 finish。仅 PASS 的审批节点生成站内抄送；
 * 退回、未达到会签通过条件以及创建待办均不生成抄送。
 */
@Component
@RequiredArgsConstructor
public class FlowCcListener implements GlobalListener {

    private final WorkflowCcMapper mapper;
    private final WorkflowIdentityProvider identityProvider;

    @Override
    public void notify(String event, ListenerVariable context) {
        if (!"finish".equals(event) || context.getTask() == null || context.getNode() == null
                || !NodeType.isBetween(context.getNode().getNodeType())
                || context.getFlowParams() == null
                || !SkipType.isPass(context.getFlowParams().getSkipType())) return;
        WorkflowCcConfig config = WorkflowCcConfig.fromExt(context.getNode().getExt());
        if (!config.enabled()) return;
        String tenantId = String.valueOf(RequestContextHolder.getTenantId());
        if (!tenantId.equals(context.getInstance().getTenantId())) {
            throw new IllegalStateException("抄送节点与当前租户不一致");
        }
        String applicantId = context.getInstance().getCreateBy();
        List<String> users = identityProvider.resolveCcUserIds(applicantId, config.recipients(),
                config.includeChildDepartments());
        if (users == null || users.isEmpty()) return;
        Map<String, WorkflowUserInfo> userInfo = identityProvider.resolveUsers(users);
        for (String userId : users.stream().distinct().toList()) {
            WorkflowCc record = new WorkflowCc();
            record.setTenantId(tenantId);
            record.setInstanceId(context.getInstance().getId());
            record.setSourceTaskId(context.getTask().getId());
            record.setNodeCode(context.getNode().getNodeCode());
            record.setNodeName(context.getNode().getNodeName());
            record.setRecipientId(userId);
            WorkflowUserInfo recipient = userInfo.get(userId);
            record.setRecipientName(recipient == null || recipient.name() == null || recipient.name().isBlank()
                    ? userId : recipient.name());
            record.setCreateTime(new Date());
            mapper.insert(record);
        }
    }

    @Override public void start(ListenerVariable context) { }
    @Override public void assignment(ListenerVariable context) { }
    @Override public void finish(ListenerVariable context) { }
    @Override public void create(ListenerVariable context) { }
}
