package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.Log;
import io.github.yilers.upm.mapper.LogMapper;
import io.github.yilers.upm.response.LogInfoResponse;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.web.context.RequestContextHolder;
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
        Long tenantId = RequestContextHolder.getContext().getTenantId();
        request.getData().setTenantId(tenantId);
        return logMapper.findByPage(p, request);
    }
}
