package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.DeptLeader;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.mapper.DeptMapper;
import io.github.yilers.upm.request.DeptLeaderSaveRequest;
import io.github.yilers.upm.response.DeptLeaderCandidateResponse;
import io.github.yilers.upm.response.DeptLeaderResponse;
import io.github.yilers.upm.service.DeptLeaderService;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 部门主管设置与展示。主管关系不自动赋予角色或部门数据权限。
 */
@Component
@RequiredArgsConstructor
public class DeptLeaderHandler {

    public static final int PRIMARY = 1;
    public static final int DEPUTY = 2;

    private final DeptLeaderService deptLeaderService;
    private final DeptService deptService;
    private final DeptMapper deptMapper;
    private final UserService userService;
    private final CommonHandler commonHandler;

    public List<DeptLeaderResponse> findCurrentByDeptId(Long deptId) {
        Dept dept = checkDeptScope(deptId);
        return toResponses(deptLeaderService.findCurrentByDeptId(deptId), Map.of(deptId, dept));
    }

    /**
     * 部门列表只展示当前用户可见部门的主管信息，不暴露租户内其他部门。
     */
    public List<DeptLeaderResponse> findVisible() {
        List<Dept> departments = commonHandler.currentDept();
        if (departments.isEmpty()) {
            return List.of();
        }
        Map<Long, Dept> deptMap = new HashMap<>();
        departments.forEach(dept -> deptMap.put(dept.getId(), dept));
        List<DeptLeader> assignments = deptLeaderService.findCurrentByDeptIds(
                new ArrayList<>(deptMap.keySet()));
        return toResponses(assignments, deptMap);
    }

    /**
     * 默认仅搜索该部门及下级员工；includeAll=true 时可搜索本租户其他员工。
     */
    public List<DeptLeaderCandidateResponse> findCandidates(Long deptId, String keyword, boolean includeAll) {
        checkDeptScope(deptId);
        Set<Long> deptIds = new HashSet<>();
        if (!includeAll) {
            deptIds.add(deptId);
            deptService.findChildById(deptId).forEach(dept -> deptIds.add(dept.getId()));
        }
        var query = Wrappers.<User>lambdaQuery()
                .select(User::getId, User::getName, User::getDeptId)
                .eq(User::getUsable, CommonConst.YES)
                .in(!includeAll, User::getDeptId, deptIds)
                .orderByAsc(User::getId)
                .last("LIMIT 100");
        if (keyword != null && !keyword.isBlank()) {
            query.like(User::getName, keyword.trim());
        }
        List<User> users = userService.list(query);
        Set<Long> candidateDeptIds = new HashSet<>();
        users.forEach(user -> {
            if (user.getDeptId() != null) {
                candidateDeptIds.add(user.getDeptId());
            }
        });
        Map<Long, Dept> deptMap = loadDeptMap(candidateDeptIds);
        return users.stream().map(user -> new DeptLeaderCandidateResponse(
                user.getId(), user.getName(), deptName(deptMap.get(user.getDeptId())))).toList();
    }

    /**
     * 替换当前有效的任职；已撤销记录保留，并以结束日期（不含当日）结束任职。
     * 同一部门在事务内锁行，防止并发设置出多个主管。
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(DeptLeaderSaveRequest request) {
        Long deptId = request.getDeptId();
        checkDeptScope(deptId);
        if (deptMapper.lockById(deptId, RequestContextHolder.getTenantId()) == null) {
            throw new CommonException("部门不存在");
        }

        Map<Long, Integer> desired = new LinkedHashMap<>();
        if (request.getPrimaryUserId() != null) {
            desired.put(request.getPrimaryUserId(), PRIMARY);
        }
        if (request.getDeputyUserIds() != null) {
            for (Long userId : request.getDeputyUserIds()) {
                if (userId == null || desired.putIfAbsent(userId, DEPUTY) != null) {
                    throw new CommonException("主管和副主管不能重复");
                }
            }
        }
        if (desired.size() > 20) {
            throw new CommonException("一个部门最多设置20位负责人");
        }
        for (Long userId : desired.keySet()) {
            User user = userService.getById(userId);
            if (user == null || !Objects.equals(user.getTenantId(), RequestContextHolder.getTenantId())
                    || !CommonConst.YES.equals(user.getUsable())) {
                throw new CommonException("主管用户不存在或已停用");
            }
        }

        LocalDate today = LocalDate.now();
        List<DeptLeader> current = deptLeaderService.findCurrentByDeptId(deptId);
        Set<Long> retained = new HashSet<>();
        for (DeptLeader leader : current) {
            if (Objects.equals(desired.get(leader.getUserId()), leader.getLeaderType())
                    && retained.add(leader.getUserId())) {
                continue;
            }
            leader.setEndDate(today);
            if (!deptLeaderService.updateById(leader)) {
                throw new CommonException("主管任职更新失败");
            }
        }
        for (Map.Entry<Long, Integer> entry : desired.entrySet()) {
            if (retained.contains(entry.getKey())) {
                continue;
            }
            DeptLeader leader = new DeptLeader();
            leader.setDeptId(deptId);
            leader.setUserId(entry.getKey());
            leader.setLeaderType(entry.getValue());
            leader.setStartDate(today);
            leader.setTenantId(RequestContextHolder.getTenantId());
            if (!deptLeaderService.save(leader)) {
                throw new CommonException("主管任职保存失败");
            }
        }
    }

    public Map<Long, List<DeptLeaderResponse>> findCurrentByUserIds(List<Long> userIds) {
        List<DeptLeader> assignments = deptLeaderService.findCurrentByUserIds(userIds);
        Set<Long> deptIds = new HashSet<>();
        assignments.forEach(leader -> deptIds.add(leader.getDeptId()));
        Map<Long, Dept> deptMap = loadDeptMap(deptIds);
        Map<Long, List<DeptLeaderResponse>> result = new HashMap<>();
        for (DeptLeaderResponse response : toResponses(assignments, deptMap)) {
            result.computeIfAbsent(response.getUserId(), ignored -> new ArrayList<>()).add(response);
        }
        return result;
    }

    private Dept checkDeptScope(Long deptId) {
        Dept dept = deptId == null ? null : deptService.getById(deptId);
        if (dept == null) {
            throw new CommonException("部门不存在");
        }
        commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), deptId);
        return dept;
    }

    private Map<Long, Dept> loadDeptMap(Set<Long> deptIds) {
        Map<Long, Dept> result = new HashMap<>();
        if (!deptIds.isEmpty()) {
            deptService.listByIds(deptIds).forEach(dept -> result.put(dept.getId(), dept));
        }
        return result;
    }

    private List<DeptLeaderResponse> toResponses(List<DeptLeader> assignments, Map<Long, Dept> deptMap) {
        if (assignments.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        assignments.forEach(leader -> userIds.add(leader.getUserId()));
        Map<Long, User> userMap = new HashMap<>();
        userService.listByIds(userIds).forEach(user -> userMap.put(user.getId(), user));
        Set<Long> userDeptIds = new HashSet<>();
        userMap.values().forEach(user -> {
            if (user.getDeptId() != null) {
                userDeptIds.add(user.getDeptId());
            }
        });
        Map<Long, Dept> userDeptMap = loadDeptMap(userDeptIds);
        return assignments.stream().map(leader -> {
            DeptLeaderResponse response = new DeptLeaderResponse();
            response.setDeptId(leader.getDeptId());
            response.setDeptName(deptName(deptMap.get(leader.getDeptId())));
            response.setUserId(leader.getUserId());
            User user = userMap.get(leader.getUserId());
            response.setUserName(user == null ? "已删除用户" : user.getName());
            response.setUserDeptName(user == null ? "已删除部门" : deptName(userDeptMap.get(user.getDeptId())));
            response.setLeaderType(leader.getLeaderType());
            response.setStartDate(leader.getStartDate());
            return response;
        }).toList();
    }

    private String deptName(Dept dept) {
        return dept == null ? "已删除部门" : dept.getDeptName();
    }
}
