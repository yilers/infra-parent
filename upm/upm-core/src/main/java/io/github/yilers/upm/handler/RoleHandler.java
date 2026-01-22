package io.github.yilers.upm.handler;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.core.enums.DataScopeEnum;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.entity.*;
import io.github.yilers.upm.request.RolePermissionRequest;
import io.github.yilers.upm.request.RoleRequest;
import io.github.yilers.upm.request.RoleUserRequest;
import io.github.yilers.upm.response.RoleInfoResponse;
import io.github.yilers.upm.service.RoleDeptService;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.web.base.BaseOperateRequest;
import io.github.yilers.web.base.BasePageRequest;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleHandler {
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;
    private final RoleDeptService roleDeptService;

    @Transactional(rollbackFor = Exception.class)
    public void save(RoleRequest roleRequest) {
        // 验证角色名称或角色编码不能重复
        Role r = roleService.findByRoleCode(roleRequest.getRoleCode());
        if (r != null) {
            throw new CommonException("角色编号重复");
        }
        // 新建角色
        Role roleNew = new Role();
        CglibUtil.copy(roleRequest, roleNew);
        roleNew.setOperable(CommonConst.YES);
        roleNew.setDeleted(CommonConst.NO);
        roleNew.setCreateTime(LocalDateTime.now());
        roleNew.setUpdateTime(LocalDateTime.now());
        roleNew.insert();

        if (DataScopeEnum.CUSTOM.getValue().equals(roleRequest.getDataScope())) {
            List<Long> roleDeptIdList = roleRequest.getRoleDeptIdList();
            if (CollUtil.isEmpty(roleDeptIdList)) {
                throw new CommonException("必须选择部门");
            }
            List<RoleDept> collect = roleDeptIdList.stream().map(item -> {
                RoleDept roleDept = new RoleDept();
                roleDept.setRoleId(roleNew.getId());
                roleDept.setDeptId(item);
                return roleDept;
            }).collect(Collectors.toList());
            roleDeptService.saveBatch(collect);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateById(RoleRequest roleRequest) {
        // 查询角色
        Role r = roleService.getById(roleRequest.getId());
        if (!StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)) {
            if (CommonConst.NO.equals(r.getOperable())) {
                throw new CommonException("该角色不可操作");
            }
        }
        if (!r.getRoleCode().equals(roleRequest.getRoleCode())) {
            Role roleOld = roleService.findByRoleCode(roleRequest.getRoleCode());
            if (roleOld != null) throw new CommonException("角色编码已存在");
        }
        if (DataScopeEnum.CUSTOM.getValue().equals(r.getDataScope())) {
            // 修改之前如果是自定义 修改后不是 则删除一下角色部门关联
            if (!DataScopeEnum.CUSTOM.getValue().equals(roleRequest.getDataScope())) {
                roleDeptService.deleteByRoleId(roleRequest.getId());
            }
        }
        // 修改信息
        Role role = new Role();
        CglibUtil.copy(roleRequest, role);
        boolean b = role.updateById();
        if (!b) {
            throw new CommonException("更新失败 数据已经变更");
        }
        if (DataScopeEnum.CUSTOM.getValue().equals(roleRequest.getDataScope())) {
            roleDeptService.deleteByRoleId(roleRequest.getId());
            List<Long> roleDeptIdList = roleRequest.getRoleDeptIdList();
            if (CollUtil.isEmpty(roleDeptIdList)) {
                throw new CommonException("必须选择部门");
            }
            List<RoleDept> collect = roleDeptIdList.stream().map(item -> {
                RoleDept roleDept = new RoleDept();
                roleDept.setRoleId(roleRequest.getId());
                roleDept.setDeptId(item);
                return roleDept;
            }).collect(Collectors.toList());
            roleDeptService.saveBatch(collect);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long roleId) {
        Role role = roleService.getById(roleId);
        if (!StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)) {
            if (CommonConst.NO.equals(role.getOperable())) {
                throw new CommonException("该角色不可操作");
            }
        }
        List<UserRole> userRoleList = userRoleService.findByRoleId(roleId);
        if (CollUtil.isNotEmpty(userRoleList)) {
            throw new CommonException("该角色下有用户，请先删除用户");
        }
        // 删除角色权限关联
        rolePermissionService.deleteByRoleId(roleId);

        roleService.removeById(roleId);

        roleDeptService.deleteByRoleId(roleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindPermission(RolePermissionRequest dto) {
        Long roleId = dto.getRoleId();
        List<Long> permissionIdList = dto.getPermissionIdList();
        String device = dto.getDevice();

        // 删除所有旧的
        rolePermissionService.deleteByRoleIdAndDevice(roleId, device);
        // 缓存删除
        String key = StrUtil.format(CommonConst.ROLE_PERMISSION_CACHE_KEY, roleId);
        SaManager.getSaTokenDao().delete(key);

        // 新增新的
        Set<RolePermission> rolePermissionSet = new LinkedHashSet<>();
        for (Long permissionId : permissionIdList) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setPermissionId(permissionId);
            rolePermission.setRoleId(roleId);
            rolePermission.setDevice(device);
            rolePermissionSet.add(rolePermission);
        }
        rolePermissionService.saveBatch(rolePermissionSet);

    }

    public RoleInfoResponse findById(Long roleId) {
        Role role = roleService.getById(roleId);
        RoleInfoResponse roleInfoResponse = new RoleInfoResponse();
        CglibUtil.copy(role, roleInfoResponse);
        List<Permission> permissionList = rolePermissionService.findPermissionListByRoleId(roleId);
        roleInfoResponse.setRelationPermissionList(permissionList);
        if (DataScopeEnum.CUSTOM.getValue().equals(role.getDataScope())) {
            Set<Long> deptIdSet = roleDeptService.findByRoleIdList(Collections.singletonList(role.getId()));
            roleInfoResponse.setRoleDeptIdList(ListUtil.of(deptIdSet));
        }
        List<Long> userIdList = userRoleService.findUserIdListByRoleId(roleId);
        roleInfoResponse.setUserIdList(userIdList);
        return roleInfoResponse;
    }

    @Transactional(rollbackFor = Exception.class)
    public void usable(BaseOperateRequest dto) {
        Long id = dto.getId();
        Role role = roleService.getById(id);
        if (!StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE)) {
            if (CommonConst.NO.equals(role.getOperable())) {
                throw new CommonException("该角色不可操作");
            }
        }
        // 切换可用状态
        role.setUsable(role.getUsable().equals(CommonConst.YES) ? CommonConst.NO : CommonConst.YES);
        role.setUpdateTime(LocalDateTime.now());
        role.updateById();
    }

    public Page<RoleInfoResponse> page(BasePageRequest<RoleRequest> request) {
        boolean b = StpUtil.hasRole(CommonConst.PLATFORM_ADMIN_ROLE_CODE);
        Page<Role> p = new Page<>(request.getCurrent(), request.getSize());
        RoleRequest data = request.getData();
        LambdaQueryWrapper<Role> query = Wrappers.lambdaQuery(Role.class);
        query.like(StrUtil.isNotBlank(data.getRoleName()), Role::getRoleName, data.getRoleName());
        query.like(StrUtil.isNotBlank(data.getRoleCode()), Role::getRoleCode, data.getRoleCode());
        query.eq(data.getUsable() != null, Role::getUsable, data.getUsable());
        query.eq(data.getDataScope() != null, Role::getDataScope, data.getDataScope());
        query.ne(!b, Role::getRoleCode, CommonConst.PLATFORM_ADMIN_ROLE_CODE);
        query.orderByAsc(Role::getId);
        Page<Role> page = roleService.page(p, query);
        Page<RoleInfoResponse> result = new Page<>(request.getCurrent(), request.getSize());
        BeanUtil.copyProperties(page, result);
        List<RoleInfoResponse> roleInfoResponseList = page.getRecords().stream().map(role -> {
            RoleInfoResponse roleInfoResponse = new RoleInfoResponse();
            CglibUtil.copy(role, roleInfoResponse);
            if (b) {
                if (CommonConst.PLATFORM_ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
                    roleInfoResponse.setOperable(role.getOperable());
                } else {
                    roleInfoResponse.setOperable(CommonConst.YES);
                }
            } else {
                if (CommonConst.TENANT_ADMIN_ROLE_CODE.equals(role.getRoleCode()) ||
                        CommonConst.DEPT_ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
                    roleInfoResponse.setOperable(CommonConst.NO);
                } else {
                    roleInfoResponse.setOperable(CommonConst.YES);
                }
            }
            return roleInfoResponse;
        }).collect(Collectors.toList());
        result.setRecords(roleInfoResponseList);
        List<RoleInfoResponse> records = result.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            List<Long> roleIdList = records.stream().map(RoleInfoResponse::getId).collect(Collectors.toList());
            Map<Long, Integer> userCountByRoleIdList = userRoleService.findUserCountByRoleIdList(roleIdList);
            records.forEach(roleInfoResponse -> {
                roleInfoResponse.setUserCount(userCountByRoleIdList.get(roleInfoResponse.getId()));
            });
        }
        return result;

    }

    @Transactional(rollbackFor = Exception.class)
    public void bindUser(RoleUserRequest request) {
        Long roleId = request.getRoleId();
        List<Long> userIdList = request.getUserIdList();
        userRoleService.deleteByRoleId(roleId);
        if (CollUtil.isNotEmpty(userIdList)) {
            List<UserRole> userRoleList = userIdList.stream().map(userId -> {
                UserRole userRole = new UserRole();
                userRole.setRoleId(roleId);
                userRole.setUserId(userId);
                return userRole;
            }).collect(Collectors.toList());
            userRoleService.saveBatch(userRoleList);
        }
    }
}
