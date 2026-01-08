package com.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.upm.entity.RoleDept;

import java.util.List;
import java.util.Set;

public interface RoleDeptService extends IService<RoleDept> {

    Set<Long> findByRoleIdList(List<Long> roleIdList);

    void deleteByRoleId(Long roleId);
}
