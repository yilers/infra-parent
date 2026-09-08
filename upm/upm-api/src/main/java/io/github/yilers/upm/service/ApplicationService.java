package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.github.yilers.upm.entity.Application;

public interface ApplicationService extends IService<Application> {
    Application findByCode(String code);
}
