package io.github.yilers.upm.service;

import io.github.yilers.upm.entity.Position;
import io.github.yilers.upm.mapper.PositionMapper;
import io.github.yilers.upm.request.PositionRequest;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PositionServiceImplTest {
    private final PositionMapper mapper = mock(PositionMapper.class);
    private final PositionServiceImpl service = new PositionServiceImpl(mapper, mock(UserService.class));

    @Test
    void reportsOptimisticLockConflict() {
        PositionRequest request = new PositionRequest();
        request.setId(10L);
        request.setPositionCode("developer");
        request.setVersion(1);
        when(mapper.updateById(any(Position.class))).thenReturn(0);

        assertThrows(CommonException.class, () -> service.updatePosition(request));
        verify(mapper).updateById(any(Position.class));
    }
}
