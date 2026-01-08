package com.github.yilers.upm.service;

import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.data.id.IdUtil;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.crypto.digest.BCrypt;
import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yilers.core.constant.CommonConst;
import com.github.yilers.core.enums.UserTypeEnum;
import com.github.yilers.upm.entity.*;
import com.github.yilers.upm.mapper.TenantMapper;
import com.github.yilers.upm.request.TenantRequest;
import com.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {
    private final TenantMapper tenantMapper;
    private final DeptService deptService;
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final PermissionService permissionService;
    private final DeviceService deviceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTenant(TenantRequest dto) {
        Tenant tenant = findByCode(dto.getCode());
        if (tenant != null) {
            throw new CommonException("租户编码已存在");
        }
        Tenant copy = CglibUtil.copy(dto, Tenant.class);
        copy.setOperable(CommonConst.YES);
        tenantMapper.insert(copy);
        Long tenantId = copy.getId();

        // 创建设备端
        Device device = initDevice(copy);
        // 创建部门
        Dept dept = initDept(copy);

        // 创建平台角色
        Role tenantRole = roleService.findByRoleCode(CommonConst.TENANT_ADMIN_ROLE_CODE);
        if (tenantRole != null) {
            Role role = new Role();
            BeanUtil.copyProperties(tenantRole, role);
            role.setTenantId(tenantId);
            role.setId(null);
            roleService.save(role);

            Long roleId = tenantRole.getId();
            List<Permission> permissionList = rolePermissionService.findPermissionListByRoleId(roleId);
            List<Permission> newList = new ArrayList<>();
            Map<Long, Permission> newPermissionMap = new HashMap<>();
            // 第一步：复制数据，生成新ID，构建映射
            for (Permission oldPerm : permissionList) {
                long newId = IdUtil.getSnowflakeNextId();
                Permission newPerm = BeanUtil.copyProperties(oldPerm, Permission.class);
                newPerm.setId(newId);
                newPerm.setTenantId(tenantId);
                // parentId 先暂时保留为旧ID，后面再统一更新
                newPermissionMap.put(oldPerm.getId(), newPerm);
            }
            // 第二步：修正 parentId
            for (Permission oldPerm : permissionList) {
                Permission newPerm = newPermissionMap.get(oldPerm.getId());
                Long oldParentId = oldPerm.getParentId();
                if (oldParentId != null && newPermissionMap.containsKey(oldParentId)) {
                    // 设置为新 parentId
                    newPerm.setParentId(newPermissionMap.get(oldParentId).getId());
                } else {
                    // 原本是顶级节点
                    newPerm.setParentId(0L);
                }
                newList.add(newPerm);
            }
            // 保存
            permissionService.saveBatch(newList);
            List<RolePermission> collect = newList.stream().map(item -> {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(role.getId());
                rolePermission.setPermissionId(item.getId());
                rolePermission.setTenantId(tenantId);
                rolePermission.setDevice(item.getDevice());
                return rolePermission;
            }).collect(Collectors.toList());
            rolePermissionService.saveBatch(collect);

            // 创建人
            String name = "租户管理员";
            User user = new User();
            user.setTenantId(tenantId);
            user.setAccount("admin@" + copy.getCode());
            user.setName(name);
            user.setPassword(BCrypt.hashpw(SecureUtil.md5(CommonConst.INIT_PWD)));
            user.setUsable(CommonConst.YES);
            user.setUserType(UserTypeEnum.ADMIN.getCode());
            user.setDeptId(dept.getId());
            user.setOperable(CommonConst.NO);
            user.setGender(CommonConst.YES);
            user.setNickname(name);
            user.setTenantId(tenantId);
            userService.save(user);

            // 添加角色用户关联
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRole.setTenantId(tenantId);
            userRoleService.save(userRole);
        }
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

    @Override
    public Tenant findByCode(String code) {
        LambdaQueryWrapper<Tenant> query = Wrappers.lambdaQuery(Tenant.class);
        query.eq(Tenant::getCode, code);
        return tenantMapper.selectOne(query);
    }

    @Override
    public void updateTenant(TenantRequest dto) {
        Tenant tenant = findByCode(dto.getCode());
        if (tenant != null && !tenant.getId().equals(dto.getId())) {
            throw new CommonException("租户编码已存在");
        }
        Tenant copy = CglibUtil.copy(dto, Tenant.class);
        copy.setOperable(CommonConst.YES);
        tenantMapper.updateById(copy);
    }
}
