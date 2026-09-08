package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.Application;
import io.github.yilers.upm.mapper.ApplicationMapper;
import org.springframework.stereotype.Service;

@Service
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {
    @Override
    public Application findByCode(String code) {
        return getOne(Wrappers.<Application>lambdaQuery().eq(Application::getCode, code));
    }
}
