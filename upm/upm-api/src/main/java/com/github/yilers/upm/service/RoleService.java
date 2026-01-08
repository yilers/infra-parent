package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.upm.entity.Role;

public interface RoleService extends IService<Role> {

    Role findByRoleCode(String roleCode);
}
