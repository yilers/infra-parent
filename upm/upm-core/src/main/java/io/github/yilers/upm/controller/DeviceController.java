package io.github.yilers.upm.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.yilers.upm.entity.Device;
import io.github.yilers.upm.service.DeviceService;
import io.github.yilers.web.base.BaseController;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
@Tag(name = "设备")
public class DeviceController extends BaseController<Device> {
    private final DeviceService deviceService;

    @Override
    protected IService<Device> getService() {
        return deviceService;
    }
}
