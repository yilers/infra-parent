package com.github.yilers.web.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestContextHolder {

    private final ThreadLocal<RequestContext> CONTEXT = new TransmittableThreadLocal<>();

    public void setContext(RequestContext context) {
        CONTEXT.set(context);
    }

    public RequestContext getContext() {
        return CONTEXT.get();
    }

    public void clear() {
        CONTEXT.remove();
    }

    public void setTenantId(Long tenantId) {
        getOrCreate().setTenantId(tenantId);
    }

    public Long getTenantId() {
        RequestContext context = CONTEXT.get();
        return context != null ? context.getTenantId() : null;
    }

    public void setDeviceType(String deviceType) {
        getOrCreate().setDeviceType(deviceType);
    }

    public String getDeviceType() {
        RequestContext context = CONTEXT.get();
        return context != null ? context.getDeviceType() : null;
    }

    private RequestContext getOrCreate() {
        RequestContext ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new RequestContext();
            CONTEXT.set(ctx);
        }
        return ctx;
    }

}
