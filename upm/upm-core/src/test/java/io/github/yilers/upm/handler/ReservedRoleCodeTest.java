package io.github.yilers.upm.handler;

import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.request.RoleRequest;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/** 已持有普通角色的用户也不能通过修改编码获得内置身份。 */
class ReservedRoleCodeTest {
    private final RoleService roles = mock(RoleService.class);
    private final RoleHandler handler = new RoleHandler(roles, mock(UserRoleService.class),
            mock(RolePermissionService.class), mock(RoleDeptService.class), mock(PermissionService.class),
            mock(ApplicationHandler.class), mock(RoleAssignmentHandler.class));

    @Test
    void rejectsReservedCodesOnCreationIncludingCaseAndWhitespaceVariants() {
        for (String code : List.of(CommonConst.PLATFORM_ADMIN_ROLE_CODE, CommonConst.TENANT_ADMIN_ROLE_CODE,
                CommonConst.DEPT_ADMIN_ROLE_CODE, "PLATFORMADMIN", " platformAdmin ")) {
            assertThrows(CommonException.class, () -> handler.save(request(code)));
        }
        verifyNoInteractions(roles);
    }

    @Test
    void rejectsRenamingIntoAndOutOfBuiltInCodesBeforeAnyWrite() {
        Role role = new Role();
        role.setId(100L);
        role.setRoleCode("employee");
        when(roles.getById(100L)).thenReturn(role);
        assertThrows(CommonException.class, () -> handler.updateById(request("platformAdmin")));
        role.setRoleCode("platformAdmin");
        assertThrows(CommonException.class, () -> handler.updateById(request("employee")));
        verify(roles, never()).findByRoleCode(anyString());
    }

    private RoleRequest request(String code) {
        RoleRequest request = new RoleRequest();
        request.setId(100L);
        request.setRoleCode(code);
        return request;
    }
}
