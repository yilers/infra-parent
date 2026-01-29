package io.github.yilers.upm.request;

import io.github.yilers.core.constant.CommonConst;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class UserPageRequest {

    @Schema(description = "账号")
    private String account;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "职位id")
    private Long positionId;
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "性别 1-男 0-女")
    private Integer gender;
    @Schema(description = "头像")
    private String photo;
    @Schema(description = "身份证号")
    private String idCard;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "手机")
    private String phone;

    private Integer usable;

    private List<Long> deptIdList;

    @Schema(description = "是否查看平台用户 1-是 0-否", hidden = true)
    private Integer lookPlatformUser = CommonConst.NO;
}
