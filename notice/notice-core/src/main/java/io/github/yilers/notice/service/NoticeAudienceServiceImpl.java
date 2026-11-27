package io.github.yilers.notice.service;

import static io.github.yilers.notice.support.NoticeSupport.*;

import io.github.yilers.notice.config.NoticeProperties;
import io.github.yilers.notice.i18n.NoticeMessageCode;
import io.github.yilers.notice.model.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

/** 在提交发布时冻结接收人；组织展开规则由通知模块负责，宿主只提供基础查询。 */
@Service
@RequiredArgsConstructor
public class NoticeAudienceServiceImpl implements NoticeAudienceService {
    /** 宿主提供的用户、部门和角色基础查询。 */
    private final NoticeDataProvider provider;

    /** 通知模块的收件人数与重试等运行配置。 */
    private final NoticeProperties properties;

    /** 校验用户、角色和部门范围，解析并去重接收用户。 */
    @Override
    public List<NoticeUser> resolve(NoticeTarget target) {
        require(target != null, NoticeMessageCode.TARGET_EMPTY);
        List<NoticeUser> users;
        if (target.all()) users = provider.findAllUsers();
        else {
            for (var entry :
                    java.util.Map.of(
                                    NoticeTypes.DirectoryType.USER,
                                    safe(target.userIds()),
                                    NoticeTypes.DirectoryType.DEPARTMENT,
                                    safe(target.departmentIds()),
                                    NoticeTypes.DirectoryType.ROLE,
                                    safe(target.roleIds()))
                            .entrySet()) {
                if (!entry.getValue().isEmpty())
                    require(
                            provider.findEntries(entry.getKey(), entry.getValue()).size()
                                    == new HashSet<>(entry.getValue()).size(),
                            NoticeMessageCode.INVALID,
                            "recipientIds");
            }
            Set<Long> ids = new LinkedHashSet<>(safe(target.userIds()));
            List<Long> departmentIds = safe(target.departmentIds());
            if (target.includeChildDepartments() && !departmentIds.isEmpty()) {
                Set<Long> departments = new LinkedHashSet<>(departmentIds);
                List<NoticeDepartment> all = provider.findDepartments();
                boolean changed;
                do {
                    changed = false;
                    for (NoticeDepartment department : all)
                        if (departments.contains(department.parentId())) changed |= departments.add(department.id());
                } while (changed);
                departmentIds = List.copyOf(departments);
            }
            if (!departmentIds.isEmpty()) ids.addAll(provider.findUserIdsByDepartmentIds(departmentIds));
            if (!safe(target.roleIds()).isEmpty()) ids.addAll(provider.findUserIdsByRoleIds(target.roleIds()));
            require(!ids.isEmpty(), NoticeMessageCode.TARGET_EMPTY);
            users = provider.findUsersByIds(List.copyOf(ids));
        }
        Map<Long, NoticeUser> unique = new LinkedHashMap<>();
        users.forEach(user -> unique.put(user.id(), user));
        require(!unique.isEmpty(), NoticeMessageCode.TARGET_EMPTY);
        require(unique.size() <= properties.getMaxRecipients(), NoticeMessageCode.INVALID, "maxRecipients");
        return List.copyOf(unique.values());
    }

    /** 将空列表归一化为无选择，并校验ID与数量上限。 */
    private List<Long> safe(List<Long> ids) {
        if (ids == null) return List.of();
        require(
                ids.size() <= properties.getMaxRecipients() && ids.stream().allMatch(id -> id != null && id > 0),
                NoticeMessageCode.INVALID,
                "recipientIds");
        return ids;
    }
}
