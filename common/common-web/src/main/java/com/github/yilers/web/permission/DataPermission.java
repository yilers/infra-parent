package com.github.yilers.web.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限
 * 表名 字段名 表达式
 *  x    x     x    权限和字段取当前人层级
 *  √    x     x    指定了主表名 权限和字段取当前人层级
 *  √    √     x    指定了主表名和过滤字段 权限取当前人层级
 *  √    √     √    指定了主表名、过滤字段、表达式 权限拼接
 *  x    √     √    没指定主表那么就是单表或者多表都拼接 字段名和表达式拼接
 * @author zhanghui
 * @date 2023/10/18 11:06
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DataPermission {

    /**
     * 真实表名eg:t_user
     * 如果多表联查必须指定要过滤的主表
     */
    String tableName() default "";

    /**
     * 要过滤的字段名eg:tenant_id
     * 如果用了columnValue表达式 必须指定该字段
     */
    String columnName() default "dept_id";

    /**
     * spel表达式写法eg:'@pms.getCurrentManageAppIds()'
     * 如果传入该字段必须指定columnName
     */
    String columnValue() default "";

    String deptField() default "dept_id";

    String userField() default "create_id";

}
