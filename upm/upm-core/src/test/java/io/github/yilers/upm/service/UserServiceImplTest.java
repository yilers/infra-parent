package io.github.yilers.upm.service;

import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Test
    void findsAccountThroughActiveTenantQuery() {
        UserMapper mapper = mock(UserMapper.class);
        UserServiceImpl service = new UserServiceImpl(mapper);
        User expected = new User();
        when(mapper.findByAccount("admin@alibaba.com")).thenReturn(expected);

        User actual = service.findByAccount("admin@alibaba.com");

        assertSame(expected, actual);
        verify(mapper).findByAccount("admin@alibaba.com");
    }
}
