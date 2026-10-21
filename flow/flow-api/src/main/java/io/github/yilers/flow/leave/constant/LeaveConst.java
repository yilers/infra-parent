package io.github.yilers.flow.leave.constant;

/** 请假业务协议与校验边界，供请求校验和业务Service共用，不依赖流程引擎。 */
public final class LeaveConst {

    /** 审批中心中的业务类型，与可配置的流程编码不是同一个概念。 */
    public static final String BUSINESS_TYPE = "leave";
    /** 使用字符串常量，兼容Bean Validation注解对编译期常量的要求。 */
    public static final String DURATION_STEP_HOURS = "0.5";
    public static final String MAX_DURATION_HOURS = "8760";
    public static final int MAX_REASON_LENGTH = 1000;

    private LeaveConst() {
    }
}
