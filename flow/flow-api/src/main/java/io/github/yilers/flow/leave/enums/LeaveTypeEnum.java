package io.github.yilers.flow.leave.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/** 请假示例支持的类型；字符串编码作为业务协议保留，不使用枚举序号入库。 */
@Getter
@RequiredArgsConstructor
public enum LeaveTypeEnum {

    PERSONAL("PERSONAL", "事假"),
    SICK("SICK", "病假"),
    ANNUAL("ANNUAL", "年假"),
    COMP_TIME("COMP_TIME", "调休"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String name;

    public static boolean supports(String code) {
        return Arrays.stream(values()).anyMatch(type -> type.code.equals(code));
    }
}
