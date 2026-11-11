package io.github.yilers.web.log;

import java.lang.annotation.*;

/**
 * 系统日志注解
 * @author hui.zhang
 * @since 2018/12/2 下午6:16
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {

    /**
     * module 模块
     * eg:用户模块
     */
    String module() default "";

    /**
     * value 操作
     * eg:新增用户
     */
    String value() default "";

    /**
     * 是否记录方法参数。批量、文件等大请求应关闭，避免操作日志字段过大。
     */
    boolean recordParams() default true;

    /**
     * 按字段名隐藏对象、Map、数组内的同名字段（递归处理）。
     * 独立参数支持 RequestParam 的 name/value；未显式命名时依赖编译保留的方法参数名。
     * 例如 {"password"}，字段名区分大小写。
     */
    String[] hideFieldList() default {};

}
