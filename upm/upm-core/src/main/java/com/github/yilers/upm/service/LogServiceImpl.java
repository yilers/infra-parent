package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yilers.upm.entity.Log;
import com.github.yilers.upm.mapper.LogMapper;
import com.github.yilers.upm.response.LogInfoResponse;
import com.github.yilers.web.base.BasePageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {
    private final LogMapper logMapper;


    @Override
    public Page<LogInfoResponse> findByPage(BasePageRequest<Log> request) {
        Page<?> p = new Page<>(request.getCurrent(), request.getSize());
        return logMapper.findByPage(p, request);
    }
}
