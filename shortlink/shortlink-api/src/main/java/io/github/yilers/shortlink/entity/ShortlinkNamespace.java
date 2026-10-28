package io.github.yilers.shortlink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.yilers.api.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 短链路径配置。租户及审计字段由服务端维护。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shortlink_namespace")
@Schema(description = "短链路径配置")
public class ShortlinkNamespace extends BaseAllColumnDomain<ShortlinkNamespace> {
    @Schema(description = "所属短链配置ID")
    private Long configId;

    @Schema(description = "所属短链域名ID")
    private Long domainId;

    @Schema(description = "域名下的路径段，如a，不含斜杠")
    private String pathCode;

    @Schema(description = "AUTO号段生成或CUSTOM调用方指定")
    private String generateType;

    @Schema(description = "短码长度，只支持6或8")
    private Integer codeLength;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    @Schema(description = "短码算法：SEQUENTIAL为旧版顺序号，FF1_V1为密钥打散，NONE为自定义")
    private String codeAlgorithm;

    @JsonIgnore
    @ToString.Exclude
    @TableField(updateStrategy = FieldStrategy.NEVER)
    @Schema(hidden = true)
    private String codeKeyCipher;
}
