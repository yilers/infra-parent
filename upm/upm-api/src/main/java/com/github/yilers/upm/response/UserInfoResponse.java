package com.github.yilers.upm.response;

import com.github.yilers.upm.entity.Permission;
import com.github.yilers.upm.entity.Role;
import com.github.yilers.upm.entity.User;
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
}
