package io.github.yilers.flow.model;

import java.time.LocalDateTime;

/**
 * Flow 目录查询返回的中立条目，不暴露 UPM 实体。
 * 对用户条目，code 可承载登录账号；对角色、部门条目则承载对应业务编码。
 */
public record WorkflowDirectoryEntry(String id, String code, String name, WorkflowDirectoryType type,
                                     LocalDateTime createTime) {
}
