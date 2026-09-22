package io.github.yilers.web.permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记支持用户单独配置数据范围的接口。
 *
 * @author hui.zhang
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataPermissionResource {

    /**
     * 配置页面展示的业务名称。
     */
    String name();
}
