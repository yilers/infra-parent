package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.Device;
import io.github.yilers.upm.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {
    private final DeviceMapper deviceMapper;

    @Override
    public List<Device> findAll() {
        return deviceMapper.findAll();
    }
}
