package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.DeptLeader;
import io.github.yilers.upm.mapper.DeptLeaderMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 部门主管任职查询实现。
 */
@Service
public class DeptLeaderServiceImpl extends ServiceImpl<DeptLeaderMapper, DeptLeader>
        implements DeptLeaderService {

    @Override
    public List<DeptLeader> findCurrentByDeptId(Long deptId) {
        LocalDate today = LocalDate.now();
        return list(Wrappers.<DeptLeader>lambdaQuery()
                .eq(DeptLeader::getDeptId, deptId)
                .le(DeptLeader::getStartDate, today)
                .and(query -> query.isNull(DeptLeader::getEndDate)
                        .or().gt(DeptLeader::getEndDate, today))
                .orderByAsc(DeptLeader::getLeaderType, DeptLeader::getId));
    }

    @Override
    public List<DeptLeader> findCurrentByDeptIds(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        return list(Wrappers.<DeptLeader>lambdaQuery()
                .in(DeptLeader::getDeptId, deptIds)
                .le(DeptLeader::getStartDate, today)
                .and(query -> query.isNull(DeptLeader::getEndDate)
                        .or().gt(DeptLeader::getEndDate, today))
                .orderByAsc(DeptLeader::getLeaderType, DeptLeader::getDeptId));
    }

    @Override
    public List<DeptLeader> findCurrentByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        return list(Wrappers.<DeptLeader>lambdaQuery()
                .in(DeptLeader::getUserId, userIds)
                .le(DeptLeader::getStartDate, today)
                .and(query -> query.isNull(DeptLeader::getEndDate)
                        .or().gt(DeptLeader::getEndDate, today))
                .orderByAsc(DeptLeader::getLeaderType, DeptLeader::getDeptId));
    }
}
