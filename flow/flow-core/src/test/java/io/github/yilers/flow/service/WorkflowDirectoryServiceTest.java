package io.github.yilers.flow.service;

import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.flow.model.WorkflowDepartmentInfo;
import io.github.yilers.flow.model.WorkflowDirectoryPage;
import io.github.yilers.flow.model.WorkflowDirectoryType;
import io.github.yilers.flow.model.WorkflowRoleInfo;
import io.github.yilers.flow.model.WorkflowUserProfile;
import io.github.yilers.flow.service.impl.WorkflowIdentityService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证目录服务对办理人查询、动态规则和名称回显的 Flow 层组装。 */
class WorkflowDirectoryServiceTest {

    private final DirectoryDataProvider dataProvider = new DirectoryDataProvider();
    private final WorkflowIdentityService identityService = new WorkflowIdentityService(dataProvider);
    private final WorkflowDirectoryService directoryService = new WorkflowDirectoryService(dataProvider, identityService);

    @Test
    void listsDynamicRulesWithoutQueryingOrganization() {
        assertTrue(directoryService.handlerTypes().contains("动态规则"));
        var result = directoryService.searchHandlers("动态规则", null, null, 1, 10);
        assertEquals(3, result.handlerAuths().total());
        assertEquals(WorkflowAssigneeRule.APPLICANT.expression(), result.handlerAuths().rows().getFirst().storageId());
        assertNull(result.treeSelections());

        var filtered = directoryService.searchHandlers("动态规则", "上一级", null, 1, 10);
        assertEquals(1, filtered.handlerAuths().total());
        assertEquals("上一级部门主管", filtered.handlerAuths().rows().getFirst().handlerName());
        assertTrue(directoryService.searchHandlers("动态规则", null, null, 2, 10)
                .handlerAuths().rows().isEmpty());
        assertEquals(0, dataProvider.directorySearchCount);
        assertEquals(0, dataProvider.departmentReadCount);
    }

    @Test
    void resolvesSelectedNamesInBatchAndOmitsUnknownTenantData() {
        dataProvider.roles.put("20", new WorkflowRoleInfo("20", "当前租户角色"));
        List<WorkflowDirectoryService.HandlerFeedback> feedback = directoryService.resolveHandlerFeedback(
                "${wfApplicant},${wfDepartmentApprovers},${wfParentDepartmentApprovers},role:20,role:99,bad:1");

        assertEquals(List.of("申请人", "本部门副主管（无则主管）", "上一级部门主管", "当前租户角色"),
                feedback.stream().map(WorkflowDirectoryService.HandlerFeedback::handlerName).toList());
        assertEquals(1, dataProvider.roleLookupCount);
    }

    @Test
    void delegatesStaticSearchAndBuildsDepartmentTreeInFlow() {
        dataProvider.departments = List.of(
                new WorkflowDepartmentInfo("1", "总部", null),
                new WorkflowDepartmentInfo("2", "研发部", "1"));
        dataProvider.directoryPage = new WorkflowDirectoryPage(2, 10, 11, List.of());

        var result = directoryService.searchHandlers("用户", " 张 ", 2L, 2, 10);

        assertEquals(11, result.handlerAuths().total());
        assertEquals(WorkflowDirectoryType.USER, dataProvider.lastType);
        assertEquals("张", dataProvider.lastKeyword);
        assertEquals("2", dataProvider.lastDepartmentId);
        assertEquals("研发部", result.treeSelections().getFirst().children().getFirst().name());
    }

    private static final class DirectoryDataProvider implements WorkflowDataProvider {
        private final Map<String, WorkflowUserProfile> users = Map.of("10", new WorkflowUserProfile("10", "管理员", "1"));
        private final Map<String, WorkflowRoleInfo> roles = new java.util.LinkedHashMap<>();
        private List<WorkflowDepartmentInfo> departments = List.of();
        private WorkflowDirectoryPage directoryPage = new WorkflowDirectoryPage(1, 10, 0, List.of());
        private int directorySearchCount;
        private int departmentReadCount;
        private int roleLookupCount;
        private WorkflowDirectoryType lastType;
        private String lastKeyword;
        private String lastDepartmentId;

        @Override public WorkflowUserProfile currentUser() { return users.get("10"); }
        @Override public Map<String, WorkflowUserProfile> findUsersByIds(List<String> ids) {
            return ids.stream().filter(users::containsKey).collect(java.util.stream.Collectors.toMap(id -> id, users::get));
        }
        @Override public Map<String, WorkflowRoleInfo> findRolesByIds(List<String> ids) {
            roleLookupCount++;
            return ids.stream().filter(roles::containsKey).collect(java.util.stream.Collectors.toMap(id -> id, roles::get));
        }
        @Override public Map<String, WorkflowDepartmentInfo> findDepartmentsByIds(List<String> ids) { return Map.of(); }
        @Override public List<String> findUserIdsByRoleIds(List<String> ids) { return List.of(); }
        @Override public List<String> findUserIdsByDepartmentIds(List<String> ids) { return List.of(); }
        @Override public List<String> findRoleIdsByUserId(String userId) { return List.of(); }
        @Override public List<String> findDepartmentLeaderUserIds(String departmentId,
                                                                  io.github.yilers.flow.model.WorkflowLeaderType type) {
            return List.of();
        }
        @Override public List<WorkflowDepartmentInfo> findAllDepartments() {
            departmentReadCount++;
            return departments;
        }
        @Override public WorkflowDirectoryPage searchDirectory(WorkflowDirectoryType type, String keyword,
                                                               String departmentId, long pageNum, long pageSize) {
            directorySearchCount++;
            lastType = type;
            lastKeyword = keyword;
            lastDepartmentId = departmentId;
            return directoryPage;
        }
        @Override public WorkflowDirectoryPage searchTransferTargets(String keyword, String excludedUserId,
                                                                      long pageNum, long pageSize) {
            return new WorkflowDirectoryPage(pageNum, pageSize, 0, List.of());
        }
    }
}
