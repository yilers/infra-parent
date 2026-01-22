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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionHandler {
    private final PermissionService permissionService;
    private final RolePermissionService rolePermissionService;

    @Transactional(rollbackFor = Exception.class)
    public void save(PermissionRequest dto) {
        Integer permissionType = dto.getPermissionType();
        if (CommonConst.BUTTON.equals(permissionType)) {
            String permissionCode = dto.getPermissionCode();
            if (StrUtil.isBlank(permissionCode)) {
                throw new CommonException("按钮权限编码不能为空");
            }
            Permission permission = permissionService.findByPermissionCode(permissionCode);
            if (ObjUtil.isNotNull(permission)) {
                throw new CommonException("按钮权限编码已存在");
            }
        }
        Permission permission = BeanUtil.copyProperties(dto, Permission.class);
        if (ObjUtil.isNull(permission.getSortNumber())) {
            // 获取同级排序最大值
            List<Permission> permissionList = permissionService.findByParentId(dto.getParentId(), null);
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
        if (CommonConst.NO.equals(p.getOperable())) {
            throw new CommonException("该资源不可操作");
        }
        if (CommonConst.BUTTON.equals(dto.getPermissionType())) {
            String permissionCode = dto.getPermissionCode();
            if (StrUtil.isBlank(permissionCode)) {
                throw new CommonException("按钮权限编码不能为空");
            }
            Permission permission = permissionService.findByPermissionCode(permissionCode);
            if (ObjUtil.isNotNull(permission) && !permission.getId().equals(id)) {
                throw new CommonException("按钮权限编码已存在");
            }
        }
        Permission permission = new Permission();
        BeanUtil.copyProperties(dto, permission);
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
        if (CommonConst.NO.equals(up.getOperable()) || CommonConst.NO.equals(down.getOperable())) {
            throw new CommonException("该资源不可操作");
        }
        if (ObjUtil.isEmpty(up) || ObjUtil.isEmpty(down)) {
            throw new CommonException("移动数据不存在");
        }
        if (!up.getParentId().equals(down.getParentId())) {
            throw new CommonException("不能非同级移动");
        }
        Integer upSortNumber = up.getSortNumber();
        Integer downSortNumber = down.getSortNumber();
        up.setSortNumber(downSortNumber);
        down.setSortNumber(upSortNumber);
        List<Permission> list = new ArrayList<>();
        list.add(up);
        list.add(down);
        permissionService.updateBatchById(list);
    }

    public List<Permission> currentInfo(String device) {
        long userId = StpUtil.getLoginIdAsLong();
        if (StrUtil.isBlank(device)) {
            device = RequestContextHolder.getDeviceType();
        }
        return permissionService.findPermissionsByUserId(userId, device);
    }

    public List<Permission> findAll(String device) {
        if (StrUtil.isBlank(device)) {
            device = RequestContextHolder.getDeviceType();
        }
        return permissionService.findAllByDevice(device);
    }


}
