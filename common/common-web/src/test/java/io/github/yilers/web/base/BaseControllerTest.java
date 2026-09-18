package io.github.yilers.web.base;

import com.baomidou.mybatisplus.spring.service.IService;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void updateReportsOptimisticLockConflict() {
        IService<Object> service = mock(IService.class);
        Object entity = new Object();
        when(service.updateById(entity)).thenReturn(false);
        BaseController<Object> controller = new BaseController<>() {
            @Override
            protected IService<Object> getService() {
                return service;
            }
        };

        assertThrows(CommonException.class, () -> controller.updateById(entity));
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveReportsFailure() {
        IService<Object> service = mock(IService.class);
        Object entity = new Object();
        when(service.save(entity)).thenReturn(false);
        BaseController<Object> controller = new BaseController<>() {
            @Override
            protected IService<Object> getService() {
                return service;
            }
        };

        assertThrows(CommonException.class, () -> controller.save(entity));
    }
}
