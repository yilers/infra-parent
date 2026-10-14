package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.DeptLeader;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.mapper.DeptMapper;
import io.github.yilers.upm.request.DeptLeaderSaveRequest;
import io.github.yilers.upm.service.DeptLeaderService;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeptLeaderHandlerTest {

    private final DeptLeaderService leaderService = mock(DeptLeaderService.class);
    private final DeptService deptService = mock(DeptService.class);
    private final DeptMapper deptMapper = mock(DeptMapper.class);
    private final UserService userService = mock(UserService.class);
    private final CommonHandler commonHandler = mock(CommonHandler.class);
    private final DeptLeaderHandler handler = new DeptLeaderHandler(
            leaderService, deptService, deptMapper, userService, commonHandler);

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void rejectsSupervisorFromAnotherTenantBeforeWriting() {
        prepareDept();
        User foreignUser = user(99L, 2L);
        when(userService.getById(99L)).thenReturn(foreignUser);
        DeptLeaderSaveRequest request = request(99L, List.of());

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(10L);
            assertThrows(CommonException.class, () -> handler.save(request));
        }

        verify(leaderService, never()).save(any());
        verify(commonHandler).checkDataScope(10L, 20L);
    }

    @Test
    void replacingSupervisorClosesOldRecordAndPreservesHistory() {
        prepareDept();
        when(userService.getById(30L)).thenReturn(user(30L, 1L));
        DeptLeader old = new DeptLeader();
        old.setId(100L);
        old.setDeptId(20L);
        old.setUserId(40L);
        old.setLeaderType(DeptLeaderHandler.PRIMARY);
        when(leaderService.findCurrentByDeptId(20L)).thenReturn(List.of(old));
        when(leaderService.updateById(old)).thenReturn(true);
        when(leaderService.save(any())).thenReturn(true);

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(10L);
            handler.save(request(30L, List.of()));
        }

        assertEquals(LocalDate.now(), old.getEndDate());
        verify(leaderService).updateById(old);
        ArgumentCaptor<DeptLeader> captor = ArgumentCaptor.forClass(DeptLeader.class);
        verify(leaderService).save(captor.capture());
        assertEquals(30L, captor.getValue().getUserId());
        assertEquals(DeptLeaderHandler.PRIMARY, captor.getValue().getLeaderType());
        assertEquals(1L, captor.getValue().getTenantId());
    }

    @Test
    void rejectsSamePersonAsSupervisorAndDeputy() {
        prepareDept();
        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(10L);
            assertThrows(CommonException.class, () -> handler.save(request(30L, List.of(30L))));
        }
        verify(leaderService, never()).save(any());
    }

    private void prepareDept() {
        RequestContextHolder.setTenantId(1L);
        Dept dept = new Dept();
        dept.setId(20L);
        when(deptService.getById(20L)).thenReturn(dept);
        when(deptMapper.lockById(20L, 1L)).thenReturn(20L);
    }

    private User user(Long userId, Long tenantId) {
        User user = new User();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setUsable(1);
        return user;
    }

    private DeptLeaderSaveRequest request(Long primaryUserId, List<Long> deputyUserIds) {
        DeptLeaderSaveRequest request = new DeptLeaderSaveRequest();
        request.setDeptId(20L);
        request.setPrimaryUserId(primaryUserId);
        request.setDeputyUserIds(deputyUserIds);
        return request;
    }
}
