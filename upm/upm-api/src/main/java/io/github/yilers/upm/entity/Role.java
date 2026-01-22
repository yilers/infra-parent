package io.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.yilers.web.base.BaseAllColumnDomain;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("upm_role")
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseAllColumnDomain<Role> {

    @Schema(description = "角色编码")
    private String roleCode;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "角色描述")
    private String roleDesc;
    @Schema(description = "数据权限类型 1-全部 2-本部门及以下 3-本部门 4-自定义部门")
    private Integer dataScope;
    @Schema(description = "扩展字段")
    private String expand;
}
