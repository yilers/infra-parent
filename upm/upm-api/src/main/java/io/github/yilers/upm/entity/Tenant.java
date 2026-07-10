package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.yilers.api.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upm_tenant")
public class Tenant extends BaseDomain<Tenant> {

    private String name;

    private String code;

    private String description;

    private String expand;

    private Integer operable;

    private Integer usable;

    @Version
    private Integer version;

}
