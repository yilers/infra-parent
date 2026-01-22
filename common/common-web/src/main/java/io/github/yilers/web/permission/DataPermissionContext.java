package io.github.yilers.web.permission;

import lombok.Data;

@Data
public class DataPermissionContext {

    private String tableName;

    private String columnName;

    // 直接写sql，或者可以是 spel解析出来的
    private String columnValue;

    private String deptField;

    private String userField;

}
