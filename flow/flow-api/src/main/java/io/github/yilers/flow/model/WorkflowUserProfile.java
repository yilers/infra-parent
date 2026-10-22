package io.github.yilers.flow.model;

/**
 * Flow 查询用户所需的最小资料，不暴露宿主用户实体。
 *
 * @param id 用户唯一标识
 * @param name 展示名称
 * @param departmentId 所属部门标识；用户未分配部门时为空
 */
public record WorkflowUserProfile(String id, String name, String departmentId) {
}
