package io.github.yilers.auth.api;

import java.lang.annotation.*;

/** 公开入口需要自行从可信资源定位租户，不能沿用访问者的登录租户。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicTenantEndpoint {
}
