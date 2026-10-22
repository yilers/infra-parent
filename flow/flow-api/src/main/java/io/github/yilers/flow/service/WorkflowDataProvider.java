package io.github.yilers.flow.service;

import io.github.yilers.flow.model.WorkflowDepartmentInfo;
import io.github.yilers.flow.model.WorkflowDirectoryPage;
import io.github.yilers.flow.model.WorkflowDirectoryType;
import io.github.yilers.flow.model.WorkflowLeaderType;
import io.github.yilers.flow.model.WorkflowRoleInfo;
import io.github.yilers.flow.model.WorkflowUserProfile;

import java.util.List;
import java.util.Map;

/**
 * Flow 所需的基础身份和组织数据查询端口。
 * <p>
 * 实现方负责在可信的当前租户范围内查询，并排除无效账号、角色、部门及任职；
 * Flow 根据这些基础数据解析办理人、动态规则、抄送对象和目录响应。集合参数应以批量查询实现，
 * 避免微服务部署时产生逐条远程调用；目录查询使用中立分页结果，不暴露宿主的 ORM 分页类型。
 */
public interface WorkflowDataProvider {

    /** 获取已认证且属于当前租户的用户资料；未登录或账号无效时必须拒绝请求。 */
    WorkflowUserProfile currentUser();

    /** 批量查询当前租户的有效用户；不存在、停用或跨租户的用户不应返回。 */
    Map<String, WorkflowUserProfile> findUsersByIds(List<String> userIds);

    /** 批量查询当前租户的有效角色；无效角色不应返回。 */
    Map<String, WorkflowRoleInfo> findRolesByIds(List<String> roleIds);

    /** 批量查询当前租户的有效部门；无效部门不应返回。 */
    Map<String, WorkflowDepartmentInfo> findDepartmentsByIds(List<String> departmentIds);

    /** 批量查询有效角色对应的当前租户有效用户ID。 */
    List<String> findUserIdsByRoleIds(List<String> roleIds);

    /** 批量查询有效部门中的当前租户有效用户ID。 */
    List<String> findUserIdsByDepartmentIds(List<String> departmentIds);

    /** 查询当前租户有效用户所属的有效角色ID。 */
    List<String> findRoleIdsByUserId(String userId);

    /** 查询指定部门及任职类型下的有效主管用户ID。 */
    List<String> findDepartmentLeaderUserIds(String departmentId, WorkflowLeaderType leaderType);

    /** 批量查询当前租户全部有效部门，用于 Flow 计算抄送的下级部门范围。 */
    List<WorkflowDepartmentInfo> findAllDepartments();

    /** 分页查询当前租户有效的办理人目录；departmentId 仅在查询用户时用于部门筛选。 */
    WorkflowDirectoryPage searchDirectory(WorkflowDirectoryType type, String keyword, String departmentId,
                                          long pageNum, long pageSize);

    /** 分页查询当前租户有效的转办/委派目标用户，并排除当前操作人。 */
    WorkflowDirectoryPage searchTransferTargets(String keyword, String excludedUserId, long pageNum, long pageSize);
}
