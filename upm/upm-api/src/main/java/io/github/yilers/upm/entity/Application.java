package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.yilers.api.base.BaseAllColumnDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_application")
@EqualsAndHashCode(callSuper = true)
public class Application extends BaseAllColumnDomain<Application> {
    private String name;
    private String code;
    private String icon;
    private String description;
    private Integer sortNumber;
}
