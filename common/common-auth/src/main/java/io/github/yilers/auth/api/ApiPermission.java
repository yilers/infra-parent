package io.github.yilers.auth.api;

import java.lang.annotation.*;

/** 系统调用接口能力。只用于机器认证入口，不替代用户菜单权限。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiPermission {
    String code();
    String name();
    String group();
}
