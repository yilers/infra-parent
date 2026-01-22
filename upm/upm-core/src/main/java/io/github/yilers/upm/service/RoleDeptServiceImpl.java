package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.upm.entity.RoleDept;
import io.github.yilers.upm.mapper.RoleDeptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleDeptServiceImpl extends ServiceImpl<RoleDeptMapper, RoleDept> implements RoleDeptService {
    private final RoleDeptMapper roleDeptMapper;

    @Override
    public Set<Long> findByRoleIdList(List<Long> roleIdList) {
        LambdaQueryWrapper<RoleDept> query = Wrappers.lambdaQuery(RoleDept.class);
        query.in(RoleDept::getRoleId, roleIdList);
        List<RoleDept> deptList = roleDeptMapper.selectList(query);
        return deptList.stream().map(RoleDept::getDeptId).collect(Collectors.toSet());
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        LambdaUpdateWrapper<RoleDept> update = Wrappers.lambdaUpdate(RoleDept.class);
        update.eq(RoleDept::getRoleId, roleId);
        roleDeptMapper.delete(update);
    }
}
