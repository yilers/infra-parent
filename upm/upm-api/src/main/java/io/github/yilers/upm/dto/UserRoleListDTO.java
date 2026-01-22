package io.github.yilers.upm.dto;

import io.github.yilers.upm.entity.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserRoleListDTO extends Role {

    private Long userId;

}
