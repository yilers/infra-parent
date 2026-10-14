package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.github.yilers.upm.entity.DeptLeader;

import java.util.List;

/**
 * 部门主管任职查询能力，供组织管理和后续工作流身份适配使用。
 */
public interface DeptLeaderService extends IService<DeptLeader> {

    List<DeptLeader> findCurrentByDeptId(Long deptId);

    List<DeptLeader> findCurrentByDeptIds(List<Long> deptIds);

    List<DeptLeader> findCurrentByUserIds(List<Long> userIds);
}
