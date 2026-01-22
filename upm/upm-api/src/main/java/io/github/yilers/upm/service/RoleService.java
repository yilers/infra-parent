package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.yilers.upm.entity.Role;

public interface RoleService extends IService<Role> {

    Role findByRoleCode(String roleCode);
}
