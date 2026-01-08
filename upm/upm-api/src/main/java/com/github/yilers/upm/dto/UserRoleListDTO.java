package com.github.yilers.upm.dto;

import com.github.yilers.upm.entity.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserRoleListDTO extends Role {

    private Long userId;

}
