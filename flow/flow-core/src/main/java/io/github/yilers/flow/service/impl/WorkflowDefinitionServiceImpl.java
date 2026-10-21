package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.model.WorkflowDefinitionInfo;
import io.github.yilers.flow.model.WorkflowDefinitionPage;
import io.github.yilers.flow.service.WorkflowDefinitionService;
import io.github.yilers.flow.service.WorkflowIdentityProvider;
import io.github.yilers.web.exception.CommonException;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.enums.PublishStatus;
import org.dromara.warm.flow.core.utils.page.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 仅负责定义的引擎调用，接口权限由调用方所在管理模块校验。
 */
@Service
public class WorkflowDefinitionServiceImpl implements WorkflowDefinitionService {

    private final WorkflowIdentityProvider identityProvider;

    public WorkflowDefinitionServiceImpl(WorkflowIdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
    }

    @Override
    public WorkflowDefinitionPage page(String flowCode, int pageNum, int pageSize) {
        identityProvider.currentUserId();
        if (pageNum < 1 || pageSize < 1 || pageSize > 200) {
            throw new CommonException("页码必须大于0，且每页最多200条");
        }
        Definition condition = FlowEngine.newDef();
        if (flowCode != null && !flowCode.isBlank()) {
            condition.setFlowCode(flowCode.trim());
        }
        Page<Definition> page = FlowEngine.defService().page(condition, new Page<>(pageNum, pageSize));
        return new WorkflowDefinitionPage(page.getTotal(), page.getList().stream().map(this::toInfo).toList());
    }

    @Override
    public List<WorkflowDefinitionInfo> versions(String flowCode) {
        requireText(flowCode, "流程编码");
        identityProvider.currentUserId();
        return FlowEngine.defService().getByFlowCode(flowCode.trim()).stream()
                .map(this::toInfo)
                .sorted(Comparator.comparing(WorkflowDefinitionInfo::id).reversed())
                .toList();
    }

    @Override
    public WorkflowDefinitionInfo get(Long definitionId) {
        identityProvider.currentUserId();
        return toInfo(requireDefinition(definitionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(String definitionJson) {
        identityProvider.currentUserId();
        DefJson design = parseDesign(definitionJson);
        if (design.getId() != null) {
            throw new CommonException("新建流程定义不能指定ID");
        }
        requireText(design.getFlowCode(), "流程编码");
        requireText(design.getFlowName(), "流程名称");
        design.setIsPublish(PublishStatus.UNPUBLISHED.getKey());
        return FlowEngine.defService().importDef(design).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long definitionId, String definitionJson) {
        identityProvider.currentUserId();
        Definition original = requireEditable(definitionId);
        DefJson design = parseDesign(definitionJson);
        if (!Objects.equals(definitionId, design.getId())
                || !Objects.equals(original.getFlowCode(), design.getFlowCode())
                || !Objects.equals(original.getVersion(), design.getVersion())) {
            throw new CommonException("流程定义ID、编码和版本不能在编辑时变更");
        }
        design.setIsPublish(PublishStatus.UNPUBLISHED.getKey());
        try {
            FlowEngine.defService().saveDef(design, false);
        } catch (Exception e) {
            throw new CommonException("保存流程定义失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copy(Long definitionId) {
        identityProvider.currentUserId();
        requireDefinition(definitionId);
        if (!FlowEngine.defService().copyDef(definitionId)) {
            throw new CommonException("复制流程定义失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long definitionId) {
        identityProvider.currentUserId();
        requireEditable(definitionId);
        if (!FlowEngine.defService().removeDef(List.of(definitionId))) {
            throw new CommonException("删除流程定义失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importJson(String definitionJson) {
        return create(definitionJson);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publish(Long definitionId) {
        identityProvider.currentUserId();
        requireDefinition(definitionId);
        return FlowEngine.defService().publish(definitionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unpublish(Long definitionId) {
        identityProvider.currentUserId();
        requireDefinition(definitionId);
        return FlowEngine.defService().unPublish(definitionId);
    }

    @Override
    public String exportJson(Long definitionId) {
        identityProvider.currentUserId();
        requireDefinition(definitionId);
        return FlowEngine.defService().exportJson(definitionId);
    }

    @Override
    public Long findPublishedId(String flowCode) {
        requireText(flowCode, "流程编码");
        identityProvider.currentUserId();
        Definition definition = FlowEngine.defService().getPublishByFlowCode(flowCode);
        return definition == null ? null : definition.getId();
    }

    private Definition requireDefinition(Long definitionId) {
        requireId(definitionId);
        Definition definition = FlowEngine.defService().getById(definitionId);
        String tenantId = FlowEngine.tenantHandler().getTenantId();
        if (definition == null || !Objects.equals(tenantId, definition.getTenantId())) {
            throw new CommonException(404, "流程定义不存在");
        }
        return definition;
    }

    private Definition requireEditable(Long definitionId) {
        Definition definition = requireDefinition(definitionId);
        if (PublishStatus.EXPIRED.getKey().equals(definition.getIsPublish())) {
            throw new CommonException("该流程定义版本已失效，请复制新版本后修改");
        }
        if (FlowEngine.insService().selectCount(FlowEngine.newIns().setDefinitionId(definitionId)) > 0) {
            throw new CommonException("该版本已有流程实例，不能修改或删除");
        }
        if (!PublishStatus.UNPUBLISHED.getKey().equals(definition.getIsPublish())) {
            throw new CommonException("请先取消发布该流程定义版本");
        }
        return definition;
    }

    private DefJson parseDesign(String definitionJson) {
        requireText(definitionJson, "流程定义JSON");
        try {
            DefJson design = FlowEngine.jsonConvert.strToBean(definitionJson, DefJson.class);
            if (design != null) {
                if (design.getNodeList() != null) {
                    design.getNodeList().forEach(node -> {
                        WorkflowCcConfig config = WorkflowCcConfig.fromExt(node.getExt());
                        if (config.enabled() && !NodeType.isBetween(node.getNodeType())) {
                            throw new CommonException("只有审批节点可以配置自动抄送");
                        }
                    });
                }
                return design;
            }
        } catch (CommonException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CommonException("流程定义JSON格式不正确");
        }
        throw new CommonException("流程定义JSON不能为空");
    }

    private WorkflowDefinitionInfo toInfo(Definition definition) {
        return new WorkflowDefinitionInfo(definition.getId(), definition.getFlowCode(), definition.getFlowName(),
                definition.getVersion(), definition.getCategory(), definition.getIsPublish(),
                definition.getActivityStatus(), definition.getCreateTime(), definition.getUpdateTime());
    }

    private static void requireId(Long definitionId) {
        if (definitionId == null || definitionId <= 0) {
            throw new IllegalArgumentException("流程定义ID必须为正数");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
    }
}
