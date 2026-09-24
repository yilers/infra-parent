package io.github.yilers.upm.response;

import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserInfoResponse extends User {

    private String deptCode;
    private String deptName;
    private String positionCode;
    private String positionName;

    private List<Role> roleList;

    private List<Permission> permissionList;

    @Schema(description = "是否仍在使用初始化密码")
    private Boolean initPwd;
}
