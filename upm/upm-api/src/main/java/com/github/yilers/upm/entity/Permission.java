package com.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.yilers.web.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_permission")
@EqualsAndHashCode(callSuper = true)
public class Permission extends BaseAllColumnDomain<Permission> {

    @Schema(description = "父id")
    private Long parentId;

    @Schema(description = "icon")
    private String menuIcon;

    @Schema(description = "路由")
    private String menuUrl;

    @Schema(description = "排序")
    private Integer sortNumber;

    @Schema(description = "权限编码")
    private String permissionCode;

    @Schema(description = "权限名称")
    private String permissionName;

    @Schema(description = "类型 0-目录 1-菜单 2-按钮")
    private Integer permissionType;

    private String component;

    private Integer cache;

    private Integer link;

    private String device;
}
