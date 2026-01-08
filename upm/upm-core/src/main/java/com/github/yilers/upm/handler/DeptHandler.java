package com.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.github.yilers.core.constant.CommonConst;
import com.github.yilers.upm.entity.Dept;
import com.github.yilers.upm.entity.User;
import com.github.yilers.upm.request.DeptRequest;
import com.github.yilers.upm.request.SortMoveRequest;
import com.github.yilers.upm.service.DeptService;
import com.github.yilers.upm.service.UserService;
import com.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeptHandler {
    private final DeptService deptService;
    private final UserService userService;
    private final CommonHandler commonHandler;

    @Transactional(rollbackFor = Exception.class)
    public void save(DeptRequest dto) {
        // 查询同一父级下是否重复
        String deptCode = dto.getDeptCode();
        Dept d = deptService.findByDeptCode(deptCode);
        if (d != null) {
            throw new CommonException("部门编码重复");
        }
        Dept dept = new Dept();
        CglibUtil.copy(dto, dept);
        dept.setDeptDeep(dto.getParentDeptDeep() + 1);
        if (dept.getSortNumber() == null) {
            // 获取同级排序最大值
            List<Dept> deptList = deptService.findByParentId(dto.getParentId(), null);
            if (CollUtil.isEmpty(deptList)) {
                dept.setSortNumber(1);
            } else {
                dept.setSortNumber(deptList.size() + 1);
            }
        }
        dept.setOperable(CommonConst.YES);
        dept.setDeleted(CommonConst.NO);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        deptService.save(dept);
    }

    public List<Dept> current() {
        return commonHandler.currentDept();
    }


    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = "dept:", key = "#deptRequest.id")
    public void updateById(DeptRequest deptRequest) {
        Dept dept = deptService.getById(deptRequest.getId());
        if (dept != null) {
            if (CommonConst.NO.equals(dept.getOperable())) {
                throw new CommonException("数据不可操作");
            }
            if (!deptRequest.getDeptCode().equals(dept.getDeptCode())) {
                Dept d = deptService.findByDeptCode(deptRequest.getDeptCode());
                if (d != null)
                    throw new CommonException("部门编码重复");
            }
            commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), deptRequest.getId());
            Dept d = new Dept();
            CglibUtil.copy(deptRequest, d);
            boolean b = deptService.updateById(d);
            if (!b) {
                throw new CommonException("更新失败 数据已经变更");
            }
        } else {
            throw new CommonException("数据不存在");
        }
    }

    public void deleteById(Long id) {
        Dept dept = deptService.getById(id);
        if (dept != null) {
            if (CommonConst.NO.equals(dept.getOperable())) {
                throw new CommonException("数据不可操作");
            }
            List<User> userList = userService.findByDeptId(id);
            if (CollUtil.isNotEmpty(userList)) {
                throw new CommonException("部门下有用户，不能删除");
            }
            List<Dept> deptList = deptService.findByParentId(dept.getId(), null);
            if (CollUtil.isNotEmpty(deptList)) {
                throw new CommonException("部门下有子部门，不能删除");
            }
            commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), id);
        } else {
            throw new CommonException("id不存在");
        }
        deptService.removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void sortOrder(SortMoveRequest dto) {
        Long fId = dto.getFId();
        Long sId = dto.getSId();
        List<Dept> deptList = deptService.findByIdList(ListUtil.of(fId, sId));
        if (deptList.size() != 2) {
            throw new CommonException("数据不存在");
        }
        Dept fDept = deptList.get(0);
        Dept sDept = deptList.get(1);
        if (CommonConst.NO.equals(fDept.getOperable()) || CommonConst.NO.equals(sDept.getOperable())) {
            throw new CommonException("数据不可操作");
        }
        if (!fDept.getParentId().equals(sDept.getParentId())) {
            throw new CommonException("不同父级不能移动");
        }
        Dept dept = new Dept();
        dept.setId(fId);
        dept.setSortNumber(sDept.getSortNumber());
        deptService.updateById(dept);

        dept.setId(sId);
        dept.setSortNumber(fDept.getSortNumber());
        deptService.updateById(dept);
    }
}
