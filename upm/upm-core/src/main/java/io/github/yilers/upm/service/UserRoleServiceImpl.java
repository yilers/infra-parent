package io.github.yilers.upm.service;

import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.yilers.upm.dto.UserRoleListDTO;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.UserRole;
import io.github.yilers.upm.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {
    private final UserRoleMapper userRoleMapper;

    @Override
    @Cached(name = "userRole:", key = "#userId", expire = 3600, cacheType = CacheType.REMOTE)
    public List<Role> findRoleListByUserId(Long userId) {
        return userRoleMapper.findRoleListByUserId(userId);
    }

    @Override
    public List<UserRole> findByRoleId(Long roleId) {
        LambdaQueryWrapper<UserRole> query = Wrappers.lambdaQuery(UserRole.class);
        query.eq(UserRole::getRoleId, roleId);
        return userRoleMapper.selectList(query);
    }

    @Override
    public void saveUserRoleRelation(Long userId, List<Long> roleIdList) {
        if (CollUtil.isNotEmpty(roleIdList)) {
            Set<UserRole> userRoleSet = new LinkedHashSet<>();
            for (Long roleId : roleIdList) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleSet.add(userRole);
            }
            userRoleMapper.insertBatchSomeColumn(userRoleSet);
        }
    }

    @Override
    @CacheInvalidate(name = "userRole:", key = "#userId")
    public void deleteByUserId(Long userId) {
        LambdaUpdateWrapper<UserRole> update = Wrappers.lambdaUpdate(UserRole.class);
        update.eq(UserRole::getUserId, userId);
        userRoleMapper.delete(update);
    }

    @Override
    public Map<Long, List<Role>> findRoleByUserIdList(List<Long> userIdList) {
        if (userIdList == null || userIdList.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, List<Role>> map = new HashMap<>();
        List<UserRoleListDTO> relationList = new ArrayList<>();
        List<List<Long>> split = ListUtil.partition(userIdList, 50);
        for (List<Long> idList : split) {
            List<UserRoleListDTO> result = userRoleMapper.findRoleByUserIdList(idList);
            if (result != null) {
                relationList.addAll(result);
            }
        }
        // 按 userId 分组，避免重复 filter
        Map<Long, List<UserRoleListDTO>> tempMap = relationList.stream()
                .collect(Collectors.groupingBy(UserRoleListDTO::getUserId));
        for (Long userId : userIdList) {
            List<UserRoleListDTO> dtoList = tempMap.getOrDefault(userId, Collections.emptyList());
            List<Role> roleList = dtoList.stream().map(item -> {
                Role role = new Role();
                CglibUtil.copy(item, role);
                return role;
            }).collect(Collectors.toList());
            map.put(userId, roleList);
        }
        return map;
    }

    @Override
    public Map<Long, Integer> findUserCountByRoleIdList(List<Long> roleIdList) {
        Map<Long, Integer> map = new HashMap<>();
        LambdaQueryWrapper<UserRole> query = Wrappers.lambdaQuery(UserRole.class);
        query.in(UserRole::getRoleId, roleIdList);
        List<UserRole> userRoleList = userRoleMapper.selectList(query);
        // 先预处理 userRoleList，构建 roleId 到数量的映射
        Map<Long, Integer> roleCountMap = userRoleList.stream()
                .collect(Collectors.groupingBy(UserRole::getRoleId, Collectors.summingInt(e -> 1)));
        // 然后快速查询每个 roleId 的出现次数
        for (Long roleId : roleIdList) {
            int count = roleCountMap.getOrDefault(roleId, 0);
            map.put(roleId, count);
        }
        return map;
    }

    @Override
    public List<Long> findUserIdListByRoleId(Long roleId) {
        LambdaQueryWrapper<UserRole> query = Wrappers.lambdaQuery(UserRole.class);
        query.eq(UserRole::getRoleId, roleId);
        List<UserRole> userRoleList = userRoleMapper.selectList(query);
        return userRoleList.stream().map(UserRole::getUserId).collect(Collectors.toList());
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        LambdaUpdateWrapper<UserRole> update = Wrappers.lambdaUpdate(UserRole.class);
        update.eq(UserRole::getRoleId, roleId);
        userRoleMapper.delete(update);
    }
}
