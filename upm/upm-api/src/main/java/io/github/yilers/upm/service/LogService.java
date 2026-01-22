package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import io.github.yilers.upm.entity.Log;
import io.github.yilers.upm.response.LogInfoResponse;
import io.github.yilers.web.base.BasePageRequest;

public interface LogService extends IService<Log> {

    Page<LogInfoResponse> findByPage(BasePageRequest<Log> request);
}
