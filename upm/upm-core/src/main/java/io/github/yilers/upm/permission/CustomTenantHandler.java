package io.github.yilers.upm.permission;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import io.github.yilers.web.context.RequestContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

public class CustomTenantHandler implements TenantLineHandler {
    @Override
    public boolean ignoreTable(String tableName) {
        // Flow 表使用字符串租户ID，由 Flow 自己的租户拦截器处理。
        return tableName != null && tableName.regionMatches(true, 0, "flow_", 0, 5);
    }

    @Override
    public Expression getTenantId() {
        Long tenantId = RequestContextHolder.getTenantId();
        return new LongValue(tenantId);
    }
}
