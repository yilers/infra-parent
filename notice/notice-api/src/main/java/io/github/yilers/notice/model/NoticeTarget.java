package io.github.yilers.notice.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 指定用户、部门、角色取并集；空目标绝不能隐式解释为全员。 */
@Schema(description = "通知接收范围")
public record NoticeTarget(
        boolean all,
        List<Long> userIds,
        List<Long> departmentIds,
        List<Long> roleIds,
        boolean includeChildDepartments) {}
