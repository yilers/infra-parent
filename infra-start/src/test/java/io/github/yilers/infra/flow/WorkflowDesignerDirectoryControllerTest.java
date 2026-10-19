package io.github.yilers.infra.flow;

import io.github.yilers.flow.model.WorkflowAssigneeRule;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 动态规则目录不枚举组织成员，回显仍不得泄露其他租户的固定办理人。 */
class WorkflowDesignerDirectoryControllerTest {

    private final UserService users = mock(UserService.class);
    private final RoleService roles = mock(RoleService.class);
    private final DeptService depts = mock(DeptService.class);
    private final WorkflowDesignerDirectoryController controller = new WorkflowDesignerDirectoryController(users, roles, depts);

    @AfterEach
    void clear() {
        RequestContextHolder.clear();
    }

    @Test
    void listsSearchableDynamicRulesWithoutQueryingOrganization() {
        assertTrue(controller.handlerType().getData().contains("动态规则"));
        var result = controller.handlerResult("动态规则", null, null, 1, 10).getData();
        assertEquals(3, result.handlerAuths().total());
        assertEquals(WorkflowAssigneeRule.APPLICANT.expression(), result.handlerAuths().rows().getFirst().storageId());
        assertNull(result.treeSelections());
        var filtered = controller.handlerResult("动态规则", "上一级", null, 1, 10).getData();
        assertEquals(1, filtered.handlerAuths().total());
        assertEquals("上一级部门主管", filtered.handlerAuths().rows().getFirst().handlerName());
        assertEquals(List.of(), controller.handlerResult("动态规则", null, null, 2, 10).getData().handlerAuths().rows());
        verifyNoInteractions(users, roles, depts);
    }

    @Test
    void echoesRuleNamesAndIsolatesFixedRoleNames() {
        RequestContextHolder.setTenantId(1L);
        Role foreign = new Role();
        foreign.setTenantId(2L);
        foreign.setRoleName("不可泄露");
        when(roles.getById(20L)).thenReturn(foreign);
        var names = controller.handlerFeedback("${wfApplicant},${wfDepartmentApprovers},${wfParentDepartmentApprovers},role:20").getData();
        assertEquals(List.of("申请人", "本部门副主管（无则主管）", "上一级部门主管"),
                names.stream().map(WorkflowDesignerDirectoryController.HandlerFeedback::handlerName).toList());
    }
}
