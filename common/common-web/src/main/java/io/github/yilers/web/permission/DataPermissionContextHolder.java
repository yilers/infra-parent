package io.github.yilers.web.permission;

import java.util.List;

public class DataPermissionContextHolder {

    private static final ThreadLocal<List<DataPermissionContext>> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void set(List<DataPermissionContext> contextList) {
        CONTEXT_HOLDER.set(contextList);
    }

    public static List<DataPermissionContext> get() {
        return CONTEXT_HOLDER.get();
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
