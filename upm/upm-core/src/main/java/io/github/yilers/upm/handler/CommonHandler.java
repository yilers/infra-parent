package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.data.id.IdUtil;
import cn.hutool.v7.core.data.id.Snowflake;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.crypto.digest.BCrypt;
import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.core.enums.DataScopeEnum;
import io.github.yilers.core.enums.UserTypeEnum;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.request.TenantRequest;
import io.github.yilers.upm.service.*;
import io.github.yilers.upm.service.*;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
    private final PermissionService permissionService;
    private final TenantService tenantService;
    private final RoleService roleService;
    private final DeviceService deviceService;
    private final RolePermissionService rolePermissionService;


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

    @Transactional(rollbackFor = Exception.class)
    public void addTenant(TenantRequest dto) {
        Tenant tenant = tenantService.findByCode(dto.getCode());
        if (tenant != null) {
            throw new CommonException("租户编码已存在");
        }
        Tenant copy = CglibUtil.copy(dto, Tenant.class);
        copy.setOperable(CommonConst.YES);
        tenantService.save(copy);
        Long tenantId = copy.getId();
        // 创建设备端
        Device device = initDevice(copy);
        // 创建部门
        Dept dept = initDept(copy);
        // 创建平台角色
        initAdmin(tenantId, copy, dept);
    }

    private void initAdmin(Long tenantId, Tenant tenant, Dept dept) {
        Role platformRole = roleService.findByRoleCode(CommonConst.PLATFORM_ADMIN_ROLE_CODE);
        Role tenantRole = roleService.findByRoleCode(CommonConst.TENANT_ADMIN_ROLE_CODE);
        Role newPlatformRole = new Role();
        BeanUtil.copyProperties(platformRole, newPlatformRole);
        newPlatformRole.setTenantId(tenantId);
        newPlatformRole.setId(null);
        newPlatformRole.setVersion(1);
        roleService.save(newPlatformRole);
        List<Permission> permissionList = rolePermissionService.findPermissionListByRoleId(platformRole.getId());
        List<Permission> tenantPermissionList = rolePermissionService.findPermissionListByRoleId(tenantRole.getId());
        List<Permission> newAllList = new ArrayList<>();
        List<Permission> newPlatformList = new ArrayList<>();
        List<Permission> newTenantList = new ArrayList<>();
        Map<Long, Permission> newPermissionMap = new HashMap<>();
        Set<Long> platformPermIds = permissionList.stream().map(Permission::getId).collect(Collectors.toSet());
        Set<Long> tenantPermIds = tenantPermissionList.stream().map(Permission::getId).collect(Collectors.toSet());
        Set<Long> ids = CollUtil.unionDistinct(platformPermIds, tenantPermIds);
        Set<Permission> unionDistinct = new LinkedHashSet<>();
        for (Long id : ids) {
            Optional<Permission> optional = permissionList.stream().filter(item -> item.getId().equals(id)).findAny();
            if (optional.isPresent()) {
                Permission permission = optional.get();
                unionDistinct.add(permission);
            } else {
                Permission permission = tenantPermissionList.stream().filter(item -> item.getId().equals(id)).findFirst().get();
                unionDistinct.add(permission);
            }
        }
        // 第一步：复制数据，生成新ID，构建映射
        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
        for (Permission oldPerm : unionDistinct) {
            long newId = snowflake.next();
            Permission newPerm = BeanUtil.copyProperties(oldPerm, Permission.class);
            newPerm.setId(newId);
            newPerm.setTenantId(tenantId);
            // parentId 先暂时保留为旧ID，后面再统一更新
            newPermissionMap.put(oldPerm.getId(), newPerm);
        }
        // 第二步：修正 parentId
        for (Permission oldPerm : unionDistinct) {
            Permission newPerm = newPermissionMap.get(oldPerm.getId());
            Long oldParentId = oldPerm.getParentId();
            if (oldParentId != null && newPermissionMap.containsKey(oldParentId)) {
                // 设置为新 parentId
                newPerm.setParentId(newPermissionMap.get(oldParentId).getId());
            } else {
                // 原本是顶级节点
                newPerm.setParentId(0L);
            }
            newAllList.add(newPerm);
            if (platformPermIds.contains(oldPerm.getId())) {
                newPlatformList.add(newPerm);
            }
            if (tenantPermIds.contains(oldPerm.getId())) {
                newTenantList.add(newPerm);
            }
        }
        permissionService.saveBatch(newAllList);
        List<RolePermission> collect = newPlatformList.stream().map(item -> {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(newPlatformRole.getId());
            rolePermission.setPermissionId(item.getId());
            rolePermission.setTenantId(tenantId);
            rolePermission.setDevice(item.getDevice());
            return rolePermission;
        }).collect(Collectors.toList());
        rolePermissionService.saveBatch(collect);
        // 创建人
        String name = "平台管理员";
        User user = new User();
        user.setTenantId(tenantId);
        user.setAccount("platform@" + tenant.getCode());
        user.setName(name);
        user.setPassword(BCrypt.hashpw(SecureUtil.md5(CommonConst.INIT_PWD)));
        user.setUsable(CommonConst.YES);
        user.setUserType(UserTypeEnum.ADMIN.getCode());
        user.setDeptId(dept.getId());
        user.setOperable(CommonConst.NO);
        user.setGender(CommonConst.YES);
        user.setNickname(name);
        user.setTenantId(tenantId);
        user.setVersion(1);
        userService.save(user);
        // 添加角色用户关联
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(newPlatformRole.getId());
        userRole.setTenantId(tenantId);
        userRoleService.save(userRole);

        // 租户管理员
        Role newTenantRole = new Role();
        BeanUtil.copyProperties(tenantRole, newTenantRole);
        newTenantRole.setTenantId(tenantId);
        newTenantRole.setId(null);
        newTenantRole.setVersion(1);
        roleService.save(newTenantRole);
        List<RolePermission> tenantCollect = newTenantList.stream().map(item -> {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(newTenantRole.getId());
            rolePermission.setPermissionId(item.getId());
            rolePermission.setTenantId(tenantId);
            rolePermission.setDevice(item.getDevice());
            return rolePermission;
        }).collect(Collectors.toList());
        rolePermissionService.saveBatch(tenantCollect);
        // 创建人
        String tenantName = "租户管理员";
        User tenantUser = new User();
        tenantUser.setTenantId(tenantId);
        tenantUser.setAccount("admin@" + tenant.getCode());
        tenantUser.setName(tenantName);
        tenantUser.setPassword(BCrypt.hashpw(SecureUtil.md5(CommonConst.INIT_PWD)));
        tenantUser.setUsable(CommonConst.YES);
        tenantUser.setUserType(UserTypeEnum.ADMIN.getCode());
        tenantUser.setDeptId(dept.getId());
        tenantUser.setOperable(CommonConst.NO);
        tenantUser.setGender(CommonConst.YES);
        tenantUser.setNickname(name);
        tenantUser.setTenantId(tenantId);
        tenantUser.setVersion(1);
        userService.save(tenantUser);
        // 添加角色用户关联
        UserRole tenantUserRole = new UserRole();
        tenantUserRole.setUserId(tenantUser.getId());
        tenantUserRole.setRoleId(newTenantRole.getId());
        tenantUserRole.setTenantId(tenantId);
        userRoleService.save(tenantUserRole);
    }

    private Dept initDept(Tenant copy) {
        Dept dept = new Dept();
        dept.setTenantId(copy.getId());
        dept.setDeptCode(copy.getCode());
        dept.setDeptName(copy.getName());
        dept.setDeptDeep(1);
        dept.setSortNumber(1);
        dept.setParentId(0L);
        dept.setOperable(CommonConst.NO);
        dept.setUsable(CommonConst.YES);
        deptService.save(dept);
        return dept;
    }

    private Device initDevice(Tenant copy) {
        // 创建租户的设备端
        Device device = new Device();
        device.setTenantId(copy.getId());
        device.setName("web端");
        device.setCode("web");
        device.setOperable(CommonConst.NO);
        device.setUsable(CommonConst.YES);
        deviceService.save(device);
        return device;
    }

}
