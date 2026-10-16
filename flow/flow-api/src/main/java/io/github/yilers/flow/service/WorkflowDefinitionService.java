package io.github.yilers.flow.service;

import io.github.yilers.flow.model.WorkflowDefinitionInfo;
import io.github.yilers.flow.model.WorkflowDefinitionPage;

import java.util.List;

/**
 * 流程定义的编程式管理契约，供受信任的管理模块使用。
 * <p>
 * 调用方必须先校验流程定义管理权限；此接口不会直接暴露为 HTTP 路由。
 */
public interface WorkflowDefinitionService {

    /** 查询当前租户的流程定义。流程编码为精确匹配，空值查询全部。 */
    WorkflowDefinitionPage page(String flowCode, int pageNum, int pageSize);

    /** 查询当前租户某流程编码的全部版本。 */
    List<WorkflowDefinitionInfo> versions(String flowCode);

    /** 查询指定版本的摘要。 */
    WorkflowDefinitionInfo get(Long definitionId);

    /** 保存新的未发布流程定义，参数使用 Warm-Flow 1.8.9 设计器 JSON。 */
    Long create(String definitionJson);

    /** 修改未发布且尚无实例的定义版本。 */
    void update(Long definitionId, String definitionJson);

    /** 从已有版本复制出新的未发布版本。 */
    void copy(Long definitionId);

    /** 删除未发布且没有任何实例的定义版本。 */
    void delete(Long definitionId);

    /**
     * 导入 Warm-Flow 1.8.9 格式的流程定义 JSON。发布状态由引擎和定义内容决定。
     *
     * @return 导入后的定义ID
     */
    Long importJson(String definitionJson);

    /**
     * 发布指定定义版本。
     */
    boolean publish(Long definitionId);

    /**
     * 取消发布指定定义版本。
     */
    boolean unpublish(Long definitionId);

    /**
     * 导出定义和节点数据为引擎 JSON。
     */
    String exportJson(Long definitionId);

    /**
     * 查询当前租户指定流程编码的已发布版本ID；不存在时返回 null。
     */
    Long findPublishedId(String flowCode);
}
