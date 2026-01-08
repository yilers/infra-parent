package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.upm.entity.Log;
import com.github.yilers.upm.response.LogInfoResponse;
import com.github.yilers.web.base.BasePageRequest;

public interface LogService extends IService<Log> {

    Page<LogInfoResponse> findByPage(BasePageRequest<Log> request);
}
