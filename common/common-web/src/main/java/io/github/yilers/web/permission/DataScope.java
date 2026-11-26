package io.github.yilers.web.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多条行数据权限规则，按各自真实表名匹配，以AND组合匹配到的条件。
 * @author zhanghui
 * @since 2023/12/12 14:06
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DataScope {

    DataPermission[] value();

}
