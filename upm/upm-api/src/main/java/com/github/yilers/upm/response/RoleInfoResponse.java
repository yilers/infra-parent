package com.github.yilers.upm.response;

import com.github.yilers.upm.entity.Permission;
import com.github.yilers.upm.entity.Role;
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
