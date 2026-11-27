package io.github.yilers.flow.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.yilers.flow.model.WorkflowDirectoryPage;

import java.time.LocalDateTime;
import java.util.List;

/** 流程设计器的办理人目录及任务操作时的人员查询。 */
public interface WorkflowDirectoryService {
    /** 返回设计器支持的办理人类型名称。 */
    List<String> handlerTypes();

    /** 返回办理人类型选项。 */
    List<HandlerDict> handlerDictionary();

    /** 分页查询办理人和可选部门树。 */
    HandlerResult searchHandlers(String handlerType, String keyword, Long departmentId, long pageNum, long pageSize);

    /** 批量解析已保存的办理人名称，用于回显。 */
    List<HandlerFeedback> resolveHandlerFeedback(String storageIds);

    /** 分页查询转办及委派可选人员。 */
    WorkflowDirectoryPage searchTaskTargets(String keyword, long pageNum, long pageSize);

    /** 办理人类型选项。 */
    record HandlerDict(String label, String value, List<?> childList) { }

    /** 官方设计器办理人列表行。 */
    record HandlerRow(String storageId, String handlerCode, String handlerName, String groupName,
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime) { }

    /** 办理人分页数据。 */
    record HandlerPage(List<HandlerRow> rows, long total) { }

    /** 部门树节点。 */
    record DeptTree(String id, String name, String parentId, List<DeptTree> children) { }

    /** 办理人列表及部门树。 */
    record HandlerResult(HandlerPage handlerAuths, List<DeptTree> treeSelections) { }

    /** 已选办理人名称回显。 */
    record HandlerFeedback(String storageId, String handlerName) { }
}
