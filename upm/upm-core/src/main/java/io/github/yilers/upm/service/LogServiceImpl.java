package io.github.yilers.upm.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Log;
import io.github.yilers.upm.mapper.LogMapper;
import io.github.yilers.upm.response.LogInfoResponse;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.web.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("id", "l.id"), Map.entry("create_time", "l.create_time"),
            Map.entry("create_name", "l.create_name"), Map.entry("operator_name", "l.create_name"),
            Map.entry("module", "l.module"), Map.entry("action", "l.action"),
            Map.entry("duration", "l.duration"), Map.entry("success", "l.success"),
            Map.entry("ip", "l.ip"), Map.entry("tenant_name", "t.name"));
    private final LogMapper logMapper;


    @Override
    public Page<LogInfoResponse> findByPage(BasePageRequest<Log> request) {
        Page<?> p = new Page<>(request.getCurrent(), request.getSize());
        PageSortHelper.apply(p, request, SORT_COLUMNS);
        // 只有平台管理员可以指定目标租户，其他用户始终按登录态中的租户查询。
        Long currentTenantId = RequestContextHolder.getTenantId();
        if (!StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)
                || request.getData().getTenantId() == null) {
            request.getData().setTenantId(currentTenantId);
        }
        return logMapper.findByPage(p, request);
    }
}
