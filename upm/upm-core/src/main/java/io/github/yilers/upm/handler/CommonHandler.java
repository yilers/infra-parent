package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.v7.core.collection.CollUtil;
import io.github.yilers.core.enums.DataScopeEnum;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserDataScope;
import io.github.yilers.upm.service.*;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理循环引用问题的
 * @author zhanghui
 * @since 2025/6/6 上午9:13
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class CommonHandler {
    private final UserService userService;
    private final DeptService deptService;
    private final RoleDeptService roleDeptService;
    private final UserRoleService userRoleService;
    private final UserDataScopeService userDataScopeService;

    public List<Dept> currentDept() {
        long userId = StpUtil.getLoginIdAsLong();
        List<Long> deptIdList = findDataScopeByUserId(userId, null);
        // 全部则默认为所在部门及下级
        if (deptIdList == null) {
            return deptService.list();
        } else if (deptIdList.size() == 1 && deptIdList.get(0) == -1L) {
            User user = userService.findById(userId);
            Long deptId = user.getDeptId();
            return Collections.singletonList(deptService.getById(deptId));
        } else {
            return deptService.findByIdList(deptIdList);
        }

    }


    public List<Long> findDataScopeByUserId(Long userId, String requestPath) {
        if (StrUtil.isBlank(requestPath)) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            requestPath = servletRequestAttributes.getRequest().getServletPath();
        }
        log.info("数据权限处理 当前请求路径为:{}", requestPath);
        // 查询有没有单独配置数据权限
        List<UserDataScope> userDataScopeList = userDataScopeService.findByUserId(userId);
        if (CollUtil.isNotEmpty(userDataScopeList)) {
            String finalRequestPath = requestPath;
            Optional<UserDataScope> matched = userDataScopeList.stream()
                    .filter(scope -> finalRequestPath.equals(scope.getInterfacePath()))
                    .findFirst();
            if (matched.isPresent()) {
                UserDataScope userDataScope = matched.get();
                Integer dataScope = userDataScope.getDataScope();
                User user = userService.findById(userId);
                Long deptId = user.getDeptId();
                if (DataScopeEnum.ALL.getValue().equals(dataScope)) {
                    log.info("用户:{} 单独数据权限 看全部", user.getName());
                    return null;
                } else if (DataScopeEnum.SELF_DEPT_AND_CHILD.getValue().equals(dataScope)) {
                    log.info("用户:{} 单独数据权限 看本部门及以下", user.getName());
                    Set<Long> deptIdSet = new HashSet<>();
                    List<Dept> deptList = deptService.findChildById(deptId);
                    Dept currentDept = deptService.findById(deptId);
                    deptList.forEach(dept -> deptIdSet.add(dept.getId()));
                    deptIdSet.add(currentDept.getId());
                    return new ArrayList<>(deptIdSet);
                } else if (DataScopeEnum.SELF_DEPT.getValue().equals(dataScope)) {
                    log.info("用户:{} 单独数据权限 看本部门", user.getName());
                    return Collections.singletonList(deptId);
                } else if (DataScopeEnum.CUSTOM.getValue().equals(dataScope)) {
                    log.info("用户:{} 单独数据权限 看自定义本部门", user.getName());
                    // 自定义时 拓展字段用逗号分割
                    String expand = userDataScope.getExpand();
                    return StrUtil.split(expand, StrUtil.COMMA)
                            .stream()
                            .map(Long::valueOf)
                            .collect(Collectors.toList());
                } else if (DataScopeEnum.MY_SELF.getValue().equals(dataScope)) {
                    log.info("用户:{} 单独数据权限 看仅自己", user.getName());
                    return Collections.singletonList(-1L);
                }
            }
        }
        List<Role> roleList = userRoleService.findRoleListByUserId(userId);
        if (CollUtil.isEmpty(roleList)) {
            return Collections.singletonList(-1L); // 无角色默认无权限
        }
        return judgeDataScope(userId, roleList);
    }

    public List<Long> judgeDataScope(Long userId, List<Role> roleList) {
        Set<Long> deptIdSet = new HashSet<>();
        User user = userService.findById(userId);
        Long deptId = user.getDeptId();
        boolean hasValidScope = false;
        for (Role role : roleList) {
            Integer scope = role.getDataScope();
            if (DataScopeEnum.ALL.getValue().equals(scope)) {
                // 只要有全部权限，直接返回 null
                return null;
            }
            if (DataScopeEnum.SELF_DEPT_AND_CHILD.getValue().equals(scope)) {
                List<Dept> deptList = deptService.findChildById(deptId);
                Dept currentDept = deptService.findById(deptId);
                deptList.forEach(dept -> deptIdSet.add(dept.getId()));
                deptIdSet.add(currentDept.getId());
                hasValidScope = true;
            } else if (DataScopeEnum.SELF_DEPT.getValue().equals(scope)) {
                deptIdSet.add(deptId);
                hasValidScope = true;
            } else if (DataScopeEnum.CUSTOM.getValue().equals(scope)) {
                List<Long> roleDeptIds = new ArrayList<>(roleDeptService.findByRoleIdList(Collections.singletonList(role.getId())));
                deptIdSet.addAll(roleDeptIds);
                hasValidScope = true;
            }
        }
        return hasValidScope ? new ArrayList<>(deptIdSet) : Collections.singletonList(-1L);
    }

    /**
     * 校验数据权限
     * @param currentUserId 当前人id
     * @param deptId 操作数据的部门id
     */
    public void checkDataScope(Long currentUserId, Long deptId) {
        List<Long> deptIdList = findDataScopeByUserId(currentUserId, null);
        if (CollUtil.isNotEmpty(deptIdList)) {
            if (!deptIdList.contains(deptId)) {
                throw new CommonException("越权操作");
            }
        }
    }

}
