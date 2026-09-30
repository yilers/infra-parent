package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.crypto.digest.BCrypt;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.Position;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserRole;
import io.github.yilers.upm.request.UserBatchAddRequest;
import io.github.yilers.upm.request.UserBatchRowRequest;
import io.github.yilers.upm.response.UserBatchAddResponse;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.PositionService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserBatchHandlerTest {

    private final UserService userService = mock(UserService.class);
    private final UserRoleService userRoleService = mock(UserRoleService.class);
    private final DeptService deptService = mock(DeptService.class);
    private final PositionService positionService = mock(PositionService.class);
    private final RoleService roleService = mock(RoleService.class);
    private final CommonHandler commonHandler = mock(CommonHandler.class);
    private final UserBatchHandler handler = new UserBatchHandler(userService, userRoleService, deptService,
            positionService, roleService, commonHandler);

    @Test
    void shouldOnlySaveRowsThatPassAllValidations() {
        UserBatchRowRequest valid = row("row-1", "new@yilers.com", "新用户", 10L);
        valid.setPositionId(20L);
        valid.setRoleIdList(List.of(100L));
        UserBatchRowRequest existing = row("row-2", "exists@yilers.com", "已存在", 10L);
        UserBatchRowRequest outOfScope = row("row-3", "other@yilers.com", "越权用户", 30L);
        UserBatchAddRequest request = new UserBatchAddRequest();
        request.setUsers(List.of(valid, existing, outOfScope));

        User existingUser = new User();
        existingUser.setAccount(existing.getAccount());
        when(userService.findByAccountList(anyList())).thenReturn(List.of(existingUser));
        when(deptService.listByIds(anyCollection())).thenReturn(List.of(dept(10L), dept(30L)));
        when(positionService.listByIds(anyCollection())).thenReturn(List.of(position(20L)));
        when(roleService.listByIds(anyCollection())).thenReturn(List.of(role(100L)));
        when(commonHandler.findDataScopeByUserId(999L, null)).thenReturn(List.of(10L));
        when(userService.saveBatch(anyList(), eq(100))).thenAnswer(invocation -> {
            List<User> users = invocation.getArgument(0);
            users.getFirst().setId(1000L);
            return true;
        });
        when(userRoleService.saveBatch(anyList(), eq(200))).thenReturn(true);

        UserBatchAddResponse response;
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(999L);
            response = handler.add(request);
        }

        assertEquals(3, response.getTotalCount());
        assertEquals(1, response.getSuccessCount());
        assertEquals(2, response.getFailureCount());
        assertTrue(response.getRows().get(1).getMessages().contains("账号已存在"));
        assertTrue(response.getRows().get(2).getMessages().contains("无权向该部门添加用户"));

        @SuppressWarnings("unchecked")
        var userCaptor = org.mockito.ArgumentCaptor.forClass((Class<List<User>>) (Class<?>) List.class);
        verify(userService).saveBatch(userCaptor.capture(), eq(100));
        User saved = userCaptor.getValue().getFirst();
        assertEquals("新用户", saved.getNickname());
        assertTrue(UserExpandHelper.isInitPwd(saved.getExpand()));
        assertTrue(BCrypt.checkpw(SecureUtil.md5(CommonConst.INIT_PWD), saved.getPassword()));

        @SuppressWarnings("unchecked")
        var relationCaptor = org.mockito.ArgumentCaptor.forClass((Class<List<UserRole>>) (Class<?>) List.class);
        verify(userRoleService).saveBatch(relationCaptor.capture(), eq(200));
        assertEquals(1000L, relationCaptor.getValue().getFirst().getUserId());
        assertEquals(100L, relationCaptor.getValue().getFirst().getRoleId());
    }

    @Test
    void shouldRejectEveryDuplicatedAccountInSameBatch() {
        UserBatchAddRequest request = new UserBatchAddRequest();
        request.setUsers(new ArrayList<>(List.of(
                row("row-1", "same@yilers.com", "用户一", 10L),
                row("row-2", "same@yilers.com", "用户二", 10L))));
        when(userService.findByAccountList(anyList())).thenReturn(List.of());
        when(deptService.listByIds(anyCollection())).thenReturn(List.of(dept(10L)));
        when(commonHandler.findDataScopeByUserId(999L, null)).thenReturn(null);

        UserBatchAddResponse response;
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(999L);
            response = handler.add(request);
        }

        assertEquals(0, response.getSuccessCount());
        assertEquals(2, response.getFailureCount());
        assertTrue(response.getRows().stream()
                .allMatch(row -> row.getMessages().contains("本批次账号重复")));
        verify(userService, never()).saveBatch(anyList(), eq(100));
    }

    private UserBatchRowRequest row(String key, String account, String name, Long deptId) {
        UserBatchRowRequest row = new UserBatchRowRequest();
        row.setRowKey(key);
        row.setAccount(account);
        row.setName(name);
        row.setDeptId(deptId);
        return row;
    }

    private Dept dept(Long id) {
        Dept dept = new Dept();
        dept.setId(id);
        dept.setUsable(CommonConst.YES);
        return dept;
    }

    private Position position(Long id) {
        Position position = new Position();
        position.setId(id);
        position.setUsable(CommonConst.YES);
        return position;
    }

    private Role role(Long id) {
        Role role = new Role();
        role.setId(id);
        role.setUsable(CommonConst.YES);
        return role;
    }
}
