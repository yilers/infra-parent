package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.upm.mapper.UserMapper;
import io.github.yilers.upm.mapper.LogMapper;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.entity.Log;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PageSortHelperTest {
    @Test
    void onlyFixedColumnsAndDirectionsReachPageOrders() {
        Page<?> page = new Page<>();
        BasePageRequest<?> request = request("create_time", "DESC");
        PageSortHelper.apply(page, request, Map.of("create_time", "l.create_time"));
        assertEquals("l.create_time", page.orders().getFirst().getColumn());
        assertFalse(page.orders().getFirst().isAsc());
        assertThrows(CommonException.class, () -> PageSortHelper.apply(new Page<>(),
                request("create_time; SELECT 1", "asc"), Map.of("create_time", "l.create_time")));
        assertThrows(CommonException.class, () -> PageSortHelper.apply(new Page<>(),
                request("create_time", "desc, (SELECT 1)"), Map.of("create_time", "l.create_time")));
        request.setSortOrderList(List.of());
        assertThrows(CommonException.class, () -> PageSortHelper.apply(new Page<>(), request, Map.of()));
    }

    @Test
    void bothHttpQueryServicesRejectSqlExpressionsBeforeCallingMapper() {
        UserMapper users = mock(UserMapper.class);
        UserServiceImpl userService = new UserServiceImpl(users);
        BasePageRequest<UserPageRequest> userRequest = request("(SELECT 1)", "asc");
        assertThrows(CommonException.class, () -> userService.findByPage(new Page<>(), userRequest));
        LogMapper logs = mock(LogMapper.class);
        LogServiceImpl logService = new LogServiceImpl(logs);
        BasePageRequest<Log> logRequest = request("create_time", "desc nulls last");
        assertThrows(CommonException.class, () -> logService.findByPage(logRequest));
        verifyNoInteractions(users, logs);
    }

    private <T> BasePageRequest<T> request(String field, String direction) {
        BasePageRequest<T> request = new BasePageRequest<>();
        request.setSortFieldList(List.of(field));
        request.setSortOrderList(List.of(direction));
        return request;
    }
}
