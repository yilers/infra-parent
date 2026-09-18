package io.github.yilers.upm.handler;

import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.request.SortMoveRequest;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeptOptimisticLockTest {
    private final DeptService service = mock(DeptService.class);
    private final DeptHandler handler = new DeptHandler(service, mock(UserService.class), mock(CommonHandler.class));

    @Test
    void sortingUsesLoadedVersionsAndReportsConflict() {
        Dept first = dept(10L, 1, 3);
        Dept second = dept(20L, 2, 7);
        when(service.findByIdList(List.of(10L, 20L))).thenReturn(List.of(first, second));
        when(service.updateById(first)).thenReturn(true);
        when(service.updateById(second)).thenReturn(false);
        SortMoveRequest request = new SortMoveRequest();
        request.setFId(10L);
        request.setSId(20L);

        assertThrows(CommonException.class, () -> handler.sortOrder(request));
        verify(service).updateById(first);
        verify(service).updateById(second);
    }

    private Dept dept(Long id, Integer sortNumber, Integer version) {
        Dept dept = new Dept();
        dept.setId(id);
        dept.setParentId(0L);
        dept.setSortNumber(sortNumber);
        dept.setVersion(version);
        dept.setOperable(1);
        return dept;
    }
}
