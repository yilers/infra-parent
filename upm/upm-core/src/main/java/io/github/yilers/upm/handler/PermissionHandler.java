package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.bean.BeanUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.RolePermission;
import io.github.yilers.upm.request.PermissionRequest;
import io.github.yilers.upm.request.SortMoveRequest;
import io.github.yilers.upm.service.PermissionService;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionHandler {
    private final PermissionService permissionService;
    private final RolePermissionService rolePermissionService;
    private final ApplicationHandler applicationHandler;
    private final ApplicationAccessHandler applicationAccessHandler;

    @Transactional(rollbackFor = Exception.class)
    public void save(PermissionRequest dto) {
        checkParent(dto);
        Integer permissionType = dto.getPermissionType();
        if (CommonConst.BUTTON.equals(permissionType)) {
            String permissionCode = dto.getPermissionCode();
            if (StrUtil.isBlank(permissionCode)) {
                throw new CommonException("按钮权限编码不能为空");
            }
            Permission permission = permissionService.findByPermissionCode(permissionCode, dto.getAppId());
            if (ObjUtil.isNotNull(permission)) {
                throw new CommonException("按钮权限编码已存在");
            }
        }
        Permission permission = BeanUtil.copyProperties(dto, Permission.class);
        permission.setId(null);
        if (ObjUtil.isNull(permission.getSortNumber())) {
            // 获取同级排序最大值
            List<Permission> permissionList = permissionService.findByParentId(dto.getParentId(), dto.getDevice(), dto.getAppId());
            if (CollUtil.isEmpty(permissionList)) {
                if (CommonConst.BUTTON.equals(permissionType)) {
                    permission.setSortNumber(1);
                } else {
                    permission.setSortNumber(10);
                }
            } else {
                if (CommonConst.BUTTON.equals(permissionType)) {
                    permission.setSortNumber(permissionList.size() + 1);
                } else {
                    permission.setSortNumber(10 * permissionList.size() + 10);
                }
            }
        }
        permission.setOperable(CommonConst.YES);
        permission.setDeleted(CommonConst.NO);
        permission.setVersion(1);
        permission.insert();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        // 查询 并校验
        Permission p = permissionService.getById(id);
        if (ObjUtil.isNull(p)) {
            throw new CommonException("资源不存在");
        }
        if (CommonConst.NO.equals(p.getOperable())) {
            throw new CommonException("该资源不可操作");
        }
        if (p.getSourceId() != null) {
            throw new CommonException("平台下发的菜单不能删除，可通过平台模板统一停用");
        }

        // 先判断子级有没有数据
        LambdaQueryWrapper<Permission> wrapper = new QueryWrapper<Permission>().lambda().select(Permission::getId);
        wrapper.eq(Permission::getParentId, id).eq(Permission::getDeleted, CommonConst.NO);
        long count = permissionService.count(wrapper);
        if (count > 0) {
            throw new CommonException("存在子资源，不能直接删除");
        }

        // 判断有没有角色绑定
        LambdaQueryWrapper<RolePermission> select = new QueryWrapper<RolePermission>().lambda();
        select.eq(RolePermission::getPermissionId, id);
        long roleBindingCount = rolePermissionService.count(select);
        if (roleBindingCount > 0) {
            throw new CommonException("有角色绑定，不能直接删除");
        }

        Permission permission = new Permission();
        permission.setId(id);
        permissionService.removeById(permission);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(PermissionRequest dto) {
        Long id = dto.getId();
        Permission p = permissionService.getById(id);
        if (p == null) {
            throw new CommonException("资源不存在");
        }
        if (CommonConst.NO.equals(p.getOperable())) {
            throw new CommonException("该资源不可操作");
        }
        if (CommonConst.BUTTON.equals(dto.getPermissionType())) {
            String permissionCode = dto.getPermissionCode();
            if (StrUtil.isBlank(permissionCode)) {
                throw new CommonException("按钮权限编码不能为空");
            }
            Permission permission = permissionService.findByPermissionCode(permissionCode, dto.getAppId());
            if (ObjUtil.isNotNull(permission) && !permission.getId().equals(id)) {
                throw new CommonException("按钮权限编码已存在");
            }
        }
        Permission permission = new Permission();
        BeanUtil.copyProperties(dto, permission);
        if (!Objects.equals(p.getAppId(), dto.getAppId()) || !Objects.equals(p.getDevice(), dto.getDevice())) {
            throw new CommonException("不能跨应用或设备端移动菜单");
        }
        checkParent(dto);
        boolean b = permission.updateById();
        if (!b) {
            throw new CommonException("更新失败 数据已经变更");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void sortOrder(SortMoveRequest dto) {
        Long fId = dto.getFId();
        Long sId = dto.getSId();
        Permission up = permissionService.getById(fId);
        Permission down = permissionService.getById(sId);
        if (ObjUtil.isEmpty(up) || ObjUtil.isEmpty(down)) {
            throw new CommonException("移动数据不存在");
        }
        if (CommonConst.NO.equals(up.getOperable()) || CommonConst.NO.equals(down.getOperable())) {
            throw new CommonException("该资源不可操作");
        }
        if (!Objects.equals(up.getAppId(), down.getAppId()) || !Objects.equals(up.getDevice(), down.getDevice())) {
            throw new CommonException("不能跨应用或设备端排序");
        }
        if (!up.getParentId().equals(down.getParentId())) {
            throw new CommonException("不能非同级移动");
        }
        Integer upSortNumber = up.getSortNumber();
        Integer downSortNumber = down.getSortNumber();
        up.setSortNumber(downSortNumber);
        down.setSortNumber(upSortNumber);
        if (!permissionService.updateById(up) || !permissionService.updateById(down)) {
            throw new CommonException("排序失败 数据已经变更");
        }
    }

    public List<Permission> currentInfo(String device, Long appId) {
        long userId = StpUtil.getLoginIdAsLong();
        if (StrUtil.isBlank(device)) {
            device = RequestContextHolder.getDeviceType();
        }
        Long targetAppId = resolveAppId(appId);
        return permissionService.findPermissionsByUserId(userId, device, targetAppId);
    }

    public List<Permission> findAll(String device, Long appId) {
        if (StrUtil.isBlank(device)) {
            device = RequestContextHolder.getDeviceType();
        }
        return permissionService.findAllByDevice(device, resolveAppId(appId));
    }

    private Long resolveAppId(Long appId) {
        Long currentAppId = applicationAccessHandler.currentApplication().getId();
        if (appId == null) {
            return currentAppId;
        }
        if (!currentAppId.equals(appId) && !StpUtil.hasPermission("system:menu:list")
                && !StpUtil.hasPermission("system:role:permission")) {
            throw new CommonException("无权查询其他应用菜单");
        }
        return applicationHandler.findById(appId).getId();
    }

    private void checkParent(PermissionRequest dto) {
        applicationHandler.findById(dto.getAppId());
        Long parentId = dto.getParentId();
        Set<Long> visited = new HashSet<>();
        while (parentId != null && parentId != 0L) {
            if (Objects.equals(parentId, dto.getId()) || !visited.add(parentId)) {
                throw new CommonException("不能选择自身或子菜单作为父级");
            }
            Permission parent = permissionService.getById(parentId);
            if (parent == null || !Objects.equals(parent.getAppId(), dto.getAppId())
                    || !Objects.equals(parent.getDevice(), dto.getDevice())) {
                throw new CommonException("父菜单必须属于同一应用和设备端");
            }
            if (CommonConst.BUTTON.equals(parent.getPermissionType())) {
                throw new CommonException("按钮不能作为父菜单");
            }
            parentId = parent.getParentId();
        }
    }


}
