package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 租户账号密码登录策略保存请求。
 */
@Data
@Schema(description = "租户账号密码登录策略保存请求")
public class TenantLoginPolicyRequest {

    @NotNull(message = "最大失败次数不能为空")
    @Min(value = 1, message = "最大失败次数不能小于1")
    @Max(value = 20, message = "最大失败次数不能大于20")
    @Schema(description = "统计周期内允许的最大连续失败次数", example = "3")
    private Integer maxFailures;

    @NotNull(message = "失败统计周期不能为空")
    @Min(value = 1, message = "失败统计周期不能小于1分钟")
    @Max(value = 1440, message = "失败统计周期不能大于1440分钟")
    @Schema(description = "登录失败统计周期，单位分钟", example = "10")
    private Integer failureWindowMinutes;

    @NotNull(message = "锁定时长不能为空")
    @Min(value = 1, message = "锁定时长不能小于1分钟")
    @Max(value = 1440, message = "锁定时长不能大于1440分钟")
    @Schema(description = "达到失败上限后的锁定时长，单位分钟", example = "10")
    private Integer lockDurationMinutes;

    @Schema(description = "乐观锁版本号；首次创建时不传，修改时传查询结果中的版本号", example = "1")
    private Integer version;
}
