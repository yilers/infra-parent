package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.yilers.upm.entity.Device;

import java.util.List;

public interface DeviceService extends IService<Device> {

    List<Device> findAll();
}
