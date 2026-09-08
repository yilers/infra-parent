package io.github.yilers.upm.handler;

import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserDataScope;
import io.github.yilers.upm.mapper.DeptMapper;
import io.github.yilers.upm.mapper.UserMapper;
import io.github.yilers.upm.mapper.UserDataScopeMapper;
import io.github.yilers.upm.request.UserDataScopeRequest;
import io.github.yilers.upm.request.UserDataScopeItemRequest;
import io.github.yilers.upm.service.UserDataScopeServiceImpl;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDataScopeTest {
    private final UserDataScopeMapper mapper = mock(UserDataScopeMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final DeptMapper departments = mock(DeptMapper.class);
    private final UserDataScopeServiceImpl service = spy(new UserDataScopeServiceImpl(mapper, users, departments));

    private UserDataScopeRequest request(List<UserDataScopeItemRequest> scopes) {
        UserDataScopeRequest request = new UserDataScopeRequest();
        request.setUserId(10L);
        request.setScopes(scopes);
        when(users.selectById(10L)).thenReturn(new User());
        doReturn(true).when(service).saveBatch(anyCollection());
        return request;
    }

    private UserDataScopeItemRequest item(int type) {
        UserDataScopeItemRequest item = new UserDataScopeItemRequest();
        item.setInterfacePath("/business/" + type);
        item.setDataScope(type);
        return item;
    }

    @Test
    void savesAllFiveTypesAndCustomDepartments() {
        UserDataScopeItemRequest custom = item(4);
        custom.setDeptIdList(List.of(1L, 2L, 1L));
        when(departments.selectByIds(List.of(1L, 2L))).thenReturn(List.of(new Dept(), new Dept()));
        service.bind(request(List.of(item(1), item(2), item(3), custom, item(5))));
        ArgumentCaptor<Collection<UserDataScope>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(service).saveBatch(captor.capture());
        List<UserDataScope> saved = List.copyOf(captor.getValue());
        assertEquals(List.of(1, 2, 3, 4, 5), saved.stream().map(UserDataScope::getDataScope).toList());
        assertEquals("1,2", saved.get(3).getExpand());
        assertEquals("", saved.getFirst().getExpand());
    }

    @Test
    void emptyListRestoresRoleDefaultsWithoutInserting() {
        service.bind(request(List.of()));
        verify(mapper).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(service, never()).saveBatch(anyCollection());
    }

    @Test
    void rejectsInvalidDepartmentsBeforeDeletingExistingConfiguration() {
        UserDataScopeItemRequest custom = item(4);
        assertThrows(CommonException.class, () -> service.bind(request(List.of(custom))));
        custom.setDeptIdList(List.of(999L));
        when(departments.selectByIds(List.of(999L))).thenReturn(List.of());
        assertThrows(CommonException.class, () -> service.bind(request(List.of(custom))));
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsDuplicatePathsAndForeignUser() {
        assertThrows(CommonException.class, () -> service.bind(request(List.of(item(1), item(1)))));
        UserDataScopeRequest request = request(List.of(item(5)));
        when(users.selectById(10L)).thenReturn(null);
        assertThrows(CommonException.class, () -> service.bind(request));
        verifyNoInteractions(mapper);
    }
}
