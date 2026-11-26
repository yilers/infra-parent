package io.github.yilers.web.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 行数据权限，按真实表名匹配，SQL条件自动使用查询中的表别名。
 * 未配置columnValue时按deptField/userField应用当前用户默认范围；
 * 配置columnValue时按columnName及表达式返回的ID集合过滤，覆盖默认范围。
 * tableName为空时会应用到插件处理的各张表，联表建议明确指定真实表名。
 * @author zhanghui
 * @since 2023/10/18 11:06
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
     * 返回可访问ID集合的SpEL，例如 @pms.getCurrentManageAppIds()。
     * 指定后必须提供columnName，覆盖默认部门/本人范围；空集合不允许访问任何行，
     * null或解析错误拒绝执行。支持Bean调用、真实方法参数及#p0/#a0/#arg0位置别名，
     * 不支持返回原始SQL片段。未配置时使用默认范围。
     */
    String columnValue() default "";

    String deptField() default "dept_id";

    String userField() default "create_id";

}
