package com.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.yilers.web.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_position")
@EqualsAndHashCode(callSuper = true)
public class Position extends BaseAllColumnDomain<Position> {

    @Schema(description = "编码")
    private String positionCode;

    @Schema(description = "名称")
    private String positionName;

    @Schema(description = "描述")
    private String positionDesc;

    @Schema(description = "排序")
    private Integer sortNumber;

}
