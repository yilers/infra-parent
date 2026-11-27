package io.github.yilers.notice.service;

import io.github.yilers.notice.model.*;
import io.github.yilers.notice.model.NoticeTypes.DirectoryType;

import java.util.List;

/** 通知所需的基础数据端口，实现只负责同租户查询。接收范围展开与快照组装留在notice-core。 单体调用本地Service，独立部署可替换成Feign/Dubbo，禁止直接依赖upm-core。 */
public interface NoticeDataProvider {
    /** 后台任务没有HTTP认证入口，必须重新检查当前租户是否存在且启用。 */
    boolean isTenantAvailable();

    List<NoticeUser> findAllUsers();

    List<NoticeUser> findUsersByIds(List<Long> ids);

    List<Long> findUserIdsByRoleIds(List<Long> ids);

    List<Long> findUserIdsByDepartmentIds(List<Long> ids);

    List<NoticeDepartment> findDepartments();

    List<NoticeDirectoryEntry> findEntries(DirectoryType type, List<Long> ids);

    NoticePage<NoticeDirectoryEntry> search(DirectoryType type, String keyword, int current, int size);
}
