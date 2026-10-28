package io.github.yilers.shortlink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 短链配置。租户及审计字段由服务端维护。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shortlink_config")
@Schema(description = "短链配置")
public class ShortlinkConfig extends BaseAllColumnDomain<ShortlinkConfig> {
    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "租户内唯一配置编码")
    private String code;

    @Schema(description = "URL完整链接或TEMPLATE业务编码模板")
    private String targetType;

    @Schema(description = "模板必须包含{code}，只允许HTTP或HTTPS")
    private String urlTemplate;

    @Schema(description = "默认有效秒数，空表示永久")
    private Long defaultValidSeconds;
}
