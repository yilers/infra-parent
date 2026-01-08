package com.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yilers.upm.entity.RoleColumn;
import com.github.yilers.upm.mapper.RoleColumnMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleColumnServiceImpl extends ServiceImpl<RoleColumnMapper, RoleColumn> implements RoleColumnService {
    private final RoleColumnMapper roleColumnMapper;


    @Override
    public List<RoleColumn> findByRoleIdAndTableName(List<Long> roleIds, String tableName) {
        LambdaQueryWrapper<RoleColumn> query = Wrappers.lambdaQuery(RoleColumn.class);
        query.in(RoleColumn::getRoleId, roleIds);
        query.eq(RoleColumn::getTableName, tableName);
        return roleColumnMapper.selectList(query);
    }

}
