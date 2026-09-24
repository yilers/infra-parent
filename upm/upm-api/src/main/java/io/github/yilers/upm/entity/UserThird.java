package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.yilers.api.base.BaseDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_user_third")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户第三方账号绑定关系")
public class UserThird extends BaseDomain<UserThird> {

    @Schema(description = "UPM用户ID")
    private Long userId;

    @Schema(description = "第三方认证平台编码")
    private String platform;

    @Schema(description = "第三方平台OpenId")
    private String openId;

    @Schema(description = "第三方平台UnionId")
    private String unionId;

    @Schema(description = "第三方用户公开资料JSON，不保存访问凭证")
    private String expand;

    @Schema(description = "租户ID")
    private Long tenantId;

}
