package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.Device;
import io.github.yilers.upm.mapper.DeviceMapper;
import io.github.yilers.web.exception.CommonException;
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
    public boolean save(Device entity) {
        entity.setVersion(1);
        return super.save(entity);
    }

    @Override
    public boolean updateById(Device entity) {
        if (entity.getVersion() == null) {
            throw new CommonException("版本号不能为空");
        }
        return super.updateById(entity);
    }

    @Override
    public List<Device> findAll() {
        return deviceMapper.findAll();
    }
}
