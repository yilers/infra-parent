package io.github.yilers.auth.api;

/** 系统调用身份；不保存明文密钥，不使用可继承线程变量，异步任务须显式传递身份。 */
public final class ApiCallerContext {
    private static final ThreadLocal<Caller> CURRENT = new ThreadLocal<>();

    private ApiCallerContext() {
    }

    public record Caller(Long credentialId, Long tenantId) {
    }

    public static Caller current() {
        return CURRENT.get();
    }

    public static void set(Caller caller) {
        CURRENT.set(caller);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
