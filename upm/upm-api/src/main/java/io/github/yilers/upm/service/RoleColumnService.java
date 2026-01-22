package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.yilers.upm.entity.RoleColumn;

import java.util.List;

public interface RoleColumnService extends IService<RoleColumn> {

    List<RoleColumn> findByRoleIdAndTableName(List<Long> roleIds, String tableName);

}
