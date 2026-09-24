package io.github.yilers.upm.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "租户应用菜单同步结果")
public class TenantPermissionSyncResponse {

    @Schema(description = "新增应用数量")
    private int applicationAddCount;

    @Schema(description = "更新应用数量")
    private int applicationUpdateCount;

    @Schema(description = "新增菜单按钮数量")
    private int permissionAddCount;

    @Schema(description = "更新菜单按钮数量")
    private int permissionUpdateCount;

    @Schema(description = "停用菜单按钮数量")
    private int permissionDisableCount;

    @Schema(description = "租户管理员新增权限数量")
    private int tenantAdminPermissionAddCount;

    @Schema(description = "租户管理员移除权限数量")
    private int tenantAdminPermissionRemoveCount;

    @Schema(description = "是否存在待同步变更")
    public boolean isChanged() {
        return applicationAddCount > 0 || applicationUpdateCount > 0
                || permissionAddCount > 0 || permissionUpdateCount > 0 || permissionDisableCount > 0
                || tenantAdminPermissionAddCount > 0 || tenantAdminPermissionRemoveCount > 0;
    }
}
