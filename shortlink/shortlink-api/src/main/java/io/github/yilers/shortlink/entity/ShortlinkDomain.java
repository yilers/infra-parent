package io.github.yilers.shortlink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 短链域名。租户及审计字段由服务端维护。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shortlink_domain")
@Schema(description = "短链域名")
public class ShortlinkDomain extends BaseAllColumnDomain<ShortlinkDomain> {
    @Schema(description = "全局唯一的小写域名，不含端口和路径")
    private String host;

    @Schema(description = "公开链接协议，生产使用https")
    private String scheme;

    @Schema(description = "对外端口；空表示协议默认端口，局域网开发可填写9000")
    private Integer publicPort;
}
