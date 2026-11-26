package io.github.yilers.api.field;

import java.lang.annotation.*;

/**
 * 在响应VO字段或getter声明业务字段权限，不用于数据库实体。
 * 资源编码在租户内唯一，列表与详情可复用同一编码；未授权的受保护字段默认隐藏。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface FieldPermission {
    /** 租户内唯一的业务资源编码，例如system:user。 */
    String resource();
    /** 配置页面显示的业务资源名称。 */
    String resourceName();
    /** 资源内唯一的业务字段编码，列表和详情可以复用。 */
    String field();
    /** 配置页面显示的字段名称。 */
    String name();
    /** NONE不允许配置脱敏，仅支持隐藏或完整查看。 */
    FieldMaskType mask() default FieldMaskType.NONE;
    /** 仅声明业务支持写入校验，仍需在保存入口显式调用checkUpdate。 */
    boolean writeSupported() default false;
}
