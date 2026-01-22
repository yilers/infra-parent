package io.github.yilers.upm.response;

import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleInfoResponse extends Role {

    private Integer userCount;

    private List<Permission> relationPermissionList;

    private List<Long> roleDeptIdList;

    private List<Long> userIdList;

}
