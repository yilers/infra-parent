package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.request.DeptRequest;
import io.github.yilers.upm.request.SortMoveRequest;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.DeptLeaderService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeptBoundaryTest {
    private final DeptService departments = mock(DeptService.class);
    private final CommonHandler common = mock(CommonHandler.class);
    private final DeptHandler handler = new DeptHandler(departments, mock(DeptLeaderService.class), mock(UserService.class), common);
    private MockedStatic<StpUtil> login;

    @BeforeEach
    void login() {
        login = mockStatic(StpUtil.class);
        login.when(StpUtil::getLoginIdAsLong).thenReturn(99L);
    }

    @AfterEach
    void close() { login.close(); }

    @Test
    void cannotCreateUnderMissingForeignOrOutOfScopeParent() {
        DeptRequest request = request(null, 20L);
        assertThrows(CommonException.class, () -> handler.save(request));
        when(departments.getById(20L)).thenReturn(dept(20L, 0L, 1));
        doThrow(new CommonException("越权操作")).when(common).checkDataScope(99L, 20L);
        assertThrows(CommonException.class, () -> handler.save(request));
        verify(departments, never()).save(any(Dept.class));
    }

    @Test
    void scopedAdministratorCannotCreateTopLevelDepartment() {
        when(common.findDataScopeByUserId(99L, null)).thenReturn(List.of(10L));
        assertThrows(CommonException.class, () -> handler.save(request(null, 0L)));
        verify(departments, never()).save(any(Dept.class));
    }

    @Test
    void rejectsMovingToDescendantOrOutOfScopeParentBeforeWriting() {
        when(departments.getById(10L)).thenReturn(dept(10L, 0L, 1));
        when(departments.getById(20L)).thenReturn(dept(20L, 10L, 2));
        assertThrows(CommonException.class, () -> handler.updateById(request(10L, 20L)));
        when(departments.getById(20L)).thenReturn(dept(20L, 0L, 1));
        doThrow(new CommonException("越权操作")).when(common).checkDataScope(99L, 20L);
        assertThrows(CommonException.class, () -> handler.updateById(request(10L, 20L)));
        verify(departments, never()).updateById(any(Dept.class));
    }

    @Test
    void subtreeOutsideScopePreventsMovingItsParent() {
        when(departments.getById(10L)).thenReturn(dept(10L, 0L, 1));
        when(departments.getById(20L)).thenReturn(dept(20L, 0L, 1));
        when(departments.findByParentId(10L, null)).thenReturn(List.of(dept(30L, 10L, 2)));
        doThrow(new CommonException("越权操作")).when(common).checkDataScope(99L, 30L);
        assertThrows(CommonException.class, () -> handler.updateById(request(10L, 20L)));
        verify(departments, never()).updateById(any(Dept.class));
    }

    @Test
    void movingUpdatesDescendantDepthAndIgnoresForgedClientDepth() {
        when(departments.getById(10L)).thenReturn(dept(10L, 0L, 1));
        when(departments.getById(20L)).thenReturn(dept(20L, 0L, 1));
        when(departments.findByParentId(10L, null)).thenReturn(List.of(dept(30L, 10L, 2)));
        when(departments.updateById(any(Dept.class))).thenReturn(true);
        DeptRequest request = request(10L, 20L);
        request.setParentDeptDeep(999);
        handler.updateById(request);
        ArgumentCaptor<Dept> updates = ArgumentCaptor.forClass(Dept.class);
        verify(departments, times(2)).updateById(updates.capture());
        assertEquals(2, updates.getAllValues().getFirst().getDeptDeep());
        assertEquals(3, updates.getAllValues().get(1).getDeptDeep());
        verify(departments).cleanCache(30L);
    }

    @Test
    void bothSortingTargetsMustBeWithinScope() {
        when(departments.findByIdList(List.of(10L, 20L)))
                .thenReturn(List.of(dept(10L, 0L, 1), dept(20L, 0L, 1)));
        doThrow(new CommonException("越权操作")).when(common).checkDataScope(99L, 20L);
        SortMoveRequest request = new SortMoveRequest();
        request.setFId(10L);
        request.setSId(20L);
        assertThrows(CommonException.class, () -> handler.sortOrder(request));
        verify(departments, never()).updateById(any(Dept.class));
    }

    private Dept dept(Long id, Long parentId, int depth) {
        Dept dept = new Dept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptCode("dept");
        dept.setDeptDeep(depth);
        dept.setOperable(1);
        dept.setVersion(1);
        return dept;
    }

    private DeptRequest request(Long id, Long parentId) {
        DeptRequest request = new DeptRequest();
        request.setId(id);
        request.setParentId(parentId);
        request.setDeptCode("dept");
        request.setVersion(1);
        return request;
    }
}
