package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.yilers.web.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_user_third")
@EqualsAndHashCode(callSuper = true)
public class UserThird extends BaseDomain<UserThird> {

    private Long userId;

    /**
     * 平台枚举，对应 PlatformEnum。
     * {@link io.github.yilers.core.enums.PlatformEnum}
     */
    private String platform;

    private String openId;

    private String unionId;

    private String expand;

    private Long tenantId;

}
