package io.github.yilers.flow.service.impl;

import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.model.WorkflowDepartmentInfo;
import io.github.yilers.flow.model.WorkflowDirectoryPage;
import io.github.yilers.flow.model.WorkflowDirectoryType;
import io.github.yilers.flow.model.WorkflowLeaderType;
import io.github.yilers.flow.model.WorkflowRoleInfo;
import io.github.yilers.flow.model.WorkflowUserProfile;
import io.github.yilers.flow.service.WorkflowDataProvider;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证Flow层根据宿主基础目录数据组装办理人、显示名称和动态主管规则。 */
class WorkflowIdentityServiceTest {

    private final TestWorkflowDataProvider dataProvider = new TestWorkflowDataProvider();
    private final WorkflowIdentityService service = new WorkflowIdentityService(dataProvider);

    @BeforeEach
    void setUp() {
        dataProvider.users.put("10", new WorkflowUserProfile("10", "张三", "100"));
    }

    @Test
    void assemblesRoleAndDepartmentFlagsIntoActiveUsers() {
        dataProvider.roles.put("20", new WorkflowRoleInfo("20", "审批人"));
        dataProvider.departments.put("30", new WorkflowDepartmentInfo("30", "研发部", null));
        dataProvider.userIdsByRoles.put("20", List.of("11", "12"));
        dataProvider.userIdsByDepartments.put("30", List.of("12", "13"));
        dataProvider.users.put("11", new WorkflowUserProfile("11", "李四", null));
        dataProvider.users.put("13", new WorkflowUserProfile("13", "王五", "30"));

        assertEquals(List.of("11", "13"), service.resolveUserIds(List.of("role:20", "dept:30")));
    }

    @Test
    void rejectsUnknownRolesAndMalformedFlags() {
        assertThrows(IllegalArgumentException.class, () -> service.resolveUserIds(List.of("role:20")));
        assertThrows(IllegalArgumentException.class, () -> service.resolveUserIds(List.of("unknown:10")));
        assertThrows(IllegalArgumentException.class, () -> service.resolveUserIds(List.of("user:abc")));
    }

    @Test
    void assemblesPermissionNamesInBatchesAndIncludesDynamicRuleLabels() {
        dataProvider.roles.put("20", new WorkflowRoleInfo("20", "审批角色"));
        dataProvider.departments.put("30", new WorkflowDepartmentInfo("30", "研发部", null));

        assertEquals(Map.of("${wfApplicant}", "申请人", "user:10", "张三", "10", "张三",
                        "role:20", "审批角色", "dept:30", "研发部"),
                service.resolvePermissionNames(List.of("${wfApplicant}", "user:10", "10", "role:20", "dept:30")));
    }

    @Test
    void resolvesDynamicDepartmentRulesAndExcludesApplicant() {
        dataProvider.users.put("10", new WorkflowUserProfile("10", "申请人", "100"));
        dataProvider.departments.put("100", new WorkflowDepartmentInfo("100", "研发部", "200"));
        dataProvider.departments.put("200", new WorkflowDepartmentInfo("200", "技术中心", null));
        dataProvider.leaders.put("100:DEPUTY", List.of("10", "20"));
        dataProvider.leaders.put("200:PRIMARY", List.of("30"));
        dataProvider.users.put("20", new WorkflowUserProfile("20", "副主管", "100"));
        dataProvider.users.put("30", new WorkflowUserProfile("30", "上级主管", "200"));

        assertEquals(List.of("20"), service.resolveDynamicAssignees("10", WorkflowAssigneeRule.DEPARTMENT_APPROVERS));
        assertEquals(List.of("30"), service.resolveDynamicAssignees("10", WorkflowAssigneeRule.PARENT_DEPARTMENT_APPROVERS));
        assertThrows(CommonException.class, () -> service.resolveDynamicAssignees("99", WorkflowAssigneeRule.APPLICANT));
    }

    @Test
    void resolvesCcRecipientsAcrossChildDepartments() {
        dataProvider.users.put("20", new WorkflowUserProfile("20", "部门员工", "101"));
        dataProvider.departments.put("100", new WorkflowDepartmentInfo("100", "研发部", null));
        dataProvider.allDepartments = List.of(
                new WorkflowDepartmentInfo("100", "研发部", null),
                new WorkflowDepartmentInfo("101", "后端组", "100"));
        dataProvider.userIdsByDepartments.put("100", List.of("20"));
        dataProvider.userIdsByDepartments.put("101", List.of("20"));

        assertEquals(List.of("20"), service.resolveCcUserIds("10", List.of("dept:100"), true));
    }

    private static final class TestWorkflowDataProvider implements WorkflowDataProvider {
        private final Map<String, WorkflowUserProfile> users = new LinkedHashMap<>();
        private final Map<String, WorkflowRoleInfo> roles = new LinkedHashMap<>();
        private final Map<String, WorkflowDepartmentInfo> departments = new LinkedHashMap<>();
        private final Map<String, List<String>> userIdsByRoles = new LinkedHashMap<>();
        private final Map<String, List<String>> userIdsByDepartments = new LinkedHashMap<>();
        private final Map<String, List<String>> leaders = new LinkedHashMap<>();
        private List<WorkflowDepartmentInfo> allDepartments = List.of();

        @Override
        public WorkflowUserProfile currentUser() {
            return users.get("10");
        }

        @Override
        public Map<String, WorkflowUserProfile> findUsersByIds(List<String> ids) {
            return select(users, ids);
        }

        @Override
        public Map<String, WorkflowRoleInfo> findRolesByIds(List<String> ids) {
            return select(roles, ids);
        }

        @Override
        public Map<String, WorkflowDepartmentInfo> findDepartmentsByIds(List<String> ids) {
            return select(departments, ids);
        }

        @Override
        public List<String> findUserIdsByRoleIds(List<String> ids) {
            return ids.stream().flatMap(id -> userIdsByRoles.getOrDefault(id, List.of()).stream()).distinct().toList();
        }

        @Override
        public List<String> findUserIdsByDepartmentIds(List<String> ids) {
            return ids.stream().flatMap(id -> userIdsByDepartments.getOrDefault(id, List.of()).stream()).distinct().toList();
        }

        @Override
        public List<String> findRoleIdsByUserId(String userId) {
            return List.of();
        }

        @Override
        public List<String> findDepartmentLeaderUserIds(String departmentId, WorkflowLeaderType leaderType) {
            return leaders.getOrDefault(departmentId + ":" + leaderType.name(), List.of());
        }

        @Override
        public List<WorkflowDepartmentInfo> findAllDepartments() {
            return allDepartments;
        }

        @Override
        public WorkflowDirectoryPage searchDirectory(WorkflowDirectoryType type, String keyword, String departmentId,
                                                     long pageNum, long pageSize) {
            return new WorkflowDirectoryPage(pageNum, pageSize, 0, List.of());
        }

        @Override
        public WorkflowDirectoryPage searchTransferTargets(String keyword, String excludedUserId,
                                                           long pageNum, long pageSize) {
            return new WorkflowDirectoryPage(pageNum, pageSize, 0, List.of());
        }

        private static <T> Map<String, T> select(Map<String, T> source, List<String> ids) {
            Map<String, T> selected = new LinkedHashMap<>();
            ids.forEach(id -> {
                T value = source.get(id);
                if (value != null) selected.put(id, value);
            });
            return selected;
        }
    }
}
