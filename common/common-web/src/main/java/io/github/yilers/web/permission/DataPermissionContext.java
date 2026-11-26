package io.github.yilers.web.permission;

import lombok.Data;

import java.util.List;

@Data
public class DataPermissionContext {

    private String tableName;

    private String columnName;

    /** 注解中的原始SpEL表达式；非空表示使用自定义范围，不再回退部门权限。 */
    private String columnValue;

    /** 自定义表达式已解析的ID集合；空集合表示拒绝所有行，null表示尚未提供合法结果。 */
    private List<Long> columnValues;

    private String deptField;

    private String userField;

}
