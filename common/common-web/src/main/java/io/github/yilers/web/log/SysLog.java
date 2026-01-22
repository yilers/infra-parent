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
     * 隐藏字段
     * eg:{"password"}
     */
    String[] hideFieldList() default {};

}
