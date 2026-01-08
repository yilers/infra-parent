package com.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.yilers.web.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_dept")
@EqualsAndHashCode(callSuper = true)
public class Dept extends BaseAllColumnDomain<Dept> {

    @Schema(description = "父id")
    private Long parentId;

    @Schema(description = "部门编码")
    private String deptCode;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门描述")
    private String deptDesc;

    @Schema(description = "排序")
    private Integer sortNumber;

    @Schema(description = "深度")
    private Integer deptDeep;

}
