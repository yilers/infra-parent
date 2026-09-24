package io.github.yilers.upm.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前用户第三方账号绑定状态。
 */
@Data
@Schema(description = "当前用户第三方账号绑定状态")
public class ThirdAuthBindingResponse {

    @Schema(description = "第三方认证平台编码", example = "dingTalk")
    private String platform;

    @Schema(description = "平台名称", example = "钉钉")
    private String platformName;

    @Schema(description = "当前租户是否已经启用该平台配置")
    private Boolean available;

    @Schema(description = "当前用户是否已经绑定")
    private Boolean bound;

    @Schema(description = "第三方账号昵称")
    private String nickname;

    @Schema(description = "第三方账号头像")
    private String avatarUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "绑定时间", example = "2026-09-24 10:27:12")
    private LocalDateTime bindTime;
}
