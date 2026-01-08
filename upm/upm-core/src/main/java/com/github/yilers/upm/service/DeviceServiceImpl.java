package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yilers.upm.entity.Device;
import com.github.yilers.upm.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {
}
