package io.github.yilers.upm.handler;

import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.request.PermissionRequest;
import io.github.yilers.upm.request.SortMoveRequest;
import io.github.yilers.upm.service.PermissionService;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionHandlerTest {
    private final PermissionService service = mock(PermissionService.class);
    private final PermissionHandler handler = new PermissionHandler(service, mock(RolePermissionService.class),
            mock(ApplicationHandler.class), mock(ApplicationAccessHandler.class));

    @Test
    void rejectsParentInAnotherApplicationOrDevice() {
        Permission parent = permission(10L, 2L, "web");
        when(service.getById(10L)).thenReturn(parent);
        PermissionRequest request = new PermissionRequest();
        request.setAppId(1L);
        request.setDevice("web");
        request.setParentId(10L);
        assertThrows(CommonException.class, () -> handler.save(request));
        parent.setAppId(1L);
        parent.setDevice("app");
        assertThrows(CommonException.class, () -> handler.save(request));
    }

    @Test
    void rejectsCyclesAndCrossApplicationSorting() {
        Permission first = permission(10L, 1L, "web");
        first.setParentId(20L);
        Permission second = permission(20L, 1L, "web");
        second.setParentId(10L);
        when(service.getById(10L)).thenReturn(first);
        when(service.getById(20L)).thenReturn(second);
        PermissionRequest request = new PermissionRequest();
        request.setAppId(1L);
        request.setDevice("web");
        request.setParentId(10L);
        assertThrows(CommonException.class, () -> handler.save(request));
        second.setAppId(2L);
        SortMoveRequest move = new SortMoveRequest();
        move.setFId(10L);
        move.setSId(20L);
        assertThrows(CommonException.class, () -> handler.sortOrder(move));
    }

    @Test
    void reportsConflictWhenEitherSortUpdateFails() {
        Permission first = permission(10L, 1L, "web");
        first.setParentId(0L);
        first.setSortNumber(1);
        first.setVersion(3);
        Permission second = permission(20L, 1L, "web");
        second.setParentId(0L);
        second.setSortNumber(2);
        second.setVersion(5);
        when(service.getById(10L)).thenReturn(first);
        when(service.getById(20L)).thenReturn(second);
        when(service.updateById(first)).thenReturn(true);
        when(service.updateById(second)).thenReturn(false);
        SortMoveRequest move = new SortMoveRequest();
        move.setFId(10L);
        move.setSId(20L);

        assertThrows(CommonException.class, () -> handler.sortOrder(move));
        verify(service).updateById(first);
        verify(service).updateById(second);
    }

    private Permission permission(Long id, Long appId, String device) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setAppId(appId);
        permission.setDevice(device);
        permission.setOperable(1);
        permission.setPermissionType(1);
        return permission;
    }
}
