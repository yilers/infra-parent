package com.github.yilers.upm.permission;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.github.yilers.web.context.RequestContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

public class CustomTenantHandler implements TenantLineHandler {
    @Override
    public Expression getTenantId() {
        Long tenantId = RequestContextHolder.getTenantId();
        return new LongValue(tenantId);
    }
}
