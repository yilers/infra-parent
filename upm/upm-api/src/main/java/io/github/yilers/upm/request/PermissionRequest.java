package io.github.yilers.upm.request;


import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.web.validated.Add;
import io.github.yilers.web.validated.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author hui.zhang
 * @since 2021/1/27 5:11 下午
 **/

@Data
public class PermissionRequest {

    @NotNull(message = "id不能为空", groups = Update.class)
    @Schema(description = "修改时传id")
    private Long id;

    @NotNull(message = "父级id不能为空", groups = Add.class)
    @Schema(description = "父id 不传则为0 根节点")
    private Long parentId = 0L;

    @Schema(description = "权限类型 不传默认为菜单 1-菜单 2-按钮", example = "1")
    private Integer permissionType = CommonConst.MENU;

    @NotBlank(message = "名称不能为空")
    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String permissionName;

    @Schema(description = "路由")
    private String menuUrl;

    @Schema(description = "权限编码")
    private String permissionCode;

    @Schema(description = "icon")
    private String menuIcon;

    @Schema(description = "排序")
    private Integer sortNumber;

    private String component;

    private Integer cache;

    private Integer link;

    private Integer usable;

    @NotNull(message = "版本号不能为空")
    private Integer version;

    @NotBlank(message = "设备端不能为空")
    private String device;

}
