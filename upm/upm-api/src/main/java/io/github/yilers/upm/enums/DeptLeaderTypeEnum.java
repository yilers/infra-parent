package io.github.yilers.upm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 部门主管任职类型。保持现有数据库编码不变，组织管理与工作流适配共用。 */
@Getter
@RequiredArgsConstructor
public enum DeptLeaderTypeEnum {

    PRIMARY(1, "主管"),
    DEPUTY(2, "副主管");

    private final Integer code;
    private final String name;
}
