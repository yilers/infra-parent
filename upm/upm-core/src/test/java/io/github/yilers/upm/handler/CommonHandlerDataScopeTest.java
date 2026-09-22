package io.github.yilers.upm.handler;

import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class CommonHandlerDataScopeTest {

    private final CommonHandler handler = mock(CommonHandler.class, CALLS_REAL_METHODS);

    @Test
    void allowsAllDataScope() {
        doReturn(null).when(handler).findDataScopeByUserId(10L, null);

        assertDoesNotThrow(() -> handler.checkDataScope(10L, 20L));
    }

    @Test
    void rejectsEmptyDataScope() {
        doReturn(List.of()).when(handler).findDataScopeByUserId(10L, null);

        assertThrows(CommonException.class, () -> handler.checkDataScope(10L, 20L));
    }

    @Test
    void allowsDepartmentWithinDataScope() {
        doReturn(List.of(20L, 30L)).when(handler).findDataScopeByUserId(10L, null);

        assertDoesNotThrow(() -> handler.checkDataScope(10L, 20L));
    }

    @Test
    void rejectsDepartmentOutsideDataScope() {
        doReturn(List.of(20L, 30L)).when(handler).findDataScopeByUserId(10L, null);

        assertThrows(CommonException.class, () -> handler.checkDataScope(10L, 40L));
    }
}
