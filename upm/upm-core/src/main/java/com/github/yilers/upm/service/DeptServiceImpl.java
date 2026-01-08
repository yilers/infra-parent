package com.github.yilers.upm.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yilers.core.constant.CommonConst;
import com.github.yilers.upm.entity.Dept;
import com.github.yilers.upm.mapper.DeptMapper;
import com.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {
    private final DeptMapper deptMapper;

    @Override
    public Dept findByDeptCode(String deptCode) {
        LambdaQueryWrapper<Dept> query = Wrappers.lambdaQuery(Dept.class);
        query.eq(Dept::getDeptCode, deptCode);
        return deptMapper.selectOne(query);
    }

    @Override
    public List<Dept> findByParentId(Long parentId, Integer usable) {
        LambdaQueryWrapper<Dept> query = Wrappers.lambdaQuery(Dept.class);
        query.eq(Dept::getParentId, parentId);
        query.eq(usable != null, Dept::getUsable, usable);
        return deptMapper.selectList(query);
    }

    @Override
    public List<Dept> findByIdList(List<Long> idList) {
        if (CollUtil.isEmpty(idList)) {
            throw new CommonException("部门id不能为空");
        }
        LambdaQueryWrapper<Dept> query = Wrappers.lambdaQuery(Dept.class);
        query.in(Dept::getId, idList);
        query.orderByAsc(Dept::getSortNumber);
        query.orderByAsc(Dept::getId);
        return deptMapper.selectList(query);
    }

    @Override
    public List<Dept> findChildById(Long id) {
        List<Dept> result = new ArrayList<>();
        findChildrenRecursively(id, result);
        return result;
    }

    @Override
    public List<Dept> findAllByByUsable(Integer usable) {
        LambdaQueryWrapper<Dept> query = Wrappers.lambdaQuery(Dept.class);
        query.eq(usable != null, Dept::getUsable, usable);
        query.orderByAsc(Dept::getSortNumber);
        query.orderByAsc(Dept::getId);
        return deptMapper.selectList(query);
    }

    private void findChildrenRecursively(Long parentId, List<Dept> result) {
        List<Dept> children = findByParentId(parentId, CommonConst.YES);
        if (children != null && !children.isEmpty()) {
            result.addAll(children);
            for (Dept child : children) {
                findChildrenRecursively(child.getId(), result);
            }
        }
    }
}
