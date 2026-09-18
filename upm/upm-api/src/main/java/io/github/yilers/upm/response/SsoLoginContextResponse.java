package io.github.yilers.upm.response;

import io.github.yilers.upm.entity.Permission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * SSO Client 使用 ticket 换取的可信登录上下文。
 */
@Data
@Schema(description = "SSO Client校验ticket后获得的可信登录上下文")
public class SsoLoginContextResponse {
    @Schema(description = "当前租户及应用的公开信息")
    private SsoApplicationResponse application;

    @Schema(description = "当前登录用户")
    private User user;

    @Schema(description = "当前用户在租户内拥有的有效角色")
    private List<Role> roles;

    @Schema(description = "当前应用、当前终端下允许访问的菜单和按钮")
    private List<Permission> menus;

    @Schema(description = "从菜单和按钮中提取并去重后的权限编码")
    private List<String> permissions;

    @Schema(description = "角色默认数据范围对应的部门ID列表；null表示可查看全部，[-1]表示无部门数据权限",
            nullable = true)
    private List<Long> dataScope;

    @Schema(description = "SSO登录用户信息")
    public record User(
            @Schema(description = "用户ID", example = "1") Long id,
            @Schema(description = "登录账号", example = "admin@yilers.com") String account,
            @Schema(description = "用户昵称", example = "管理员") String nickname,
            @Schema(description = "用户姓名", example = "张三") String name,
            @Schema(description = "用户头像地址") String photo,
            @Schema(description = "所属部门ID") Long deptId,
            @Schema(description = "所属职位ID") Long positionId) {
    }

    @Schema(description = "SSO登录用户角色信息")
    public record Role(
            @Schema(description = "角色ID", example = "1") Long id,
            @Schema(description = "角色编码", example = "oa_user") String code,
            @Schema(description = "角色名称", example = "OA普通用户") String name,
            @Schema(description = "数据权限类型：1-全部，2-本部门及以下，3-本部门，4-自定义部门") Integer dataScope) {
    }
}
