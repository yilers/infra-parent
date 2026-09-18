package io.github.yilers.upm.service;

import io.github.yilers.upm.entity.Device;
import io.github.yilers.upm.mapper.DeviceMapper;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class DeviceServiceImplTest {

    @Test
    void updateRequiresVersion() {
        DeviceServiceImpl service = new DeviceServiceImpl(mock(DeviceMapper.class));
        Device device = new Device();
        device.setId(10L);

        assertThrows(CommonException.class, () -> service.updateById(device));
    }
}
