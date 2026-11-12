package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.collection.ListUtil;
import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import com.alicp.jetcache.anno.CacheInvalidate;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.DeptRequest;
import io.github.yilers.upm.request.SortMoveRequest;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.DeptLeaderService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.upm.i18n.UpmManagementMessage;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeptHandler {
    private static final long ROOT_PARENT_ID = 0L;
    private final DeptService deptService;
    private final DeptLeaderService deptLeaderService;
    private final UserService userService;
    private final CommonHandler commonHandler;

    @Transactional(rollbackFor = Exception.class)
    public void save(DeptRequest dto) {
        Dept parent = checkParent(null, dto.getParentId(), true);
        // 查询同一父级下是否重复
        String deptCode = dto.getDeptCode();
        Dept d = deptService.findByDeptCode(deptCode);
        if (d != null) {
            throw new CommonException(UpmManagementMessage.DEPT_CODE_EXISTS);
        }
        Dept dept = new Dept();
        CglibUtil.copy(dto, dept);
        dept.setId(null);
        dept.setDeptDeep(parent == null ? 1 : parent.getDeptDeep() + 1);
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
        dept.setVersion(1);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        deptService.save(dept);
    }

    public List<Dept> current() {
        return commonHandler.currentDept();
    }


    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = CommonConst.DEPT_CACHE_NAME, key = "#deptRequest.id")
    public void updateById(DeptRequest deptRequest) {
        Dept dept = deptService.getById(deptRequest.getId());
        if (dept != null) {
            if (CommonConst.NO.equals(dept.getOperable())) {
                throw new CommonException(UpmManagementMessage.DATA_NOT_OPERABLE);
            }
            if (!deptRequest.getDeptCode().equals(dept.getDeptCode())) {
                Dept d = deptService.findByDeptCode(deptRequest.getDeptCode());
                if (d != null)
                    throw new CommonException(UpmManagementMessage.DEPT_CODE_EXISTS);
            }
            commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), deptRequest.getId());
            Long parentId = deptRequest.getParentId() == null ? dept.getParentId() : deptRequest.getParentId();
            boolean moving = !Objects.equals(parentId, dept.getParentId());
            Dept parent = checkParent(dept.getId(), parentId, moving);
            int depth = parent == null ? 1 : parent.getDeptDeep() + 1;
            // 移动会改变整个子树的层级，先检查全部受影响部门，再进行任何写入。
            List<Dept> descendants = moving ? descendantUpdates(dept.getId(), depth) : List.of();
            Dept d = new Dept();
            CglibUtil.copy(deptRequest, d);
            d.setParentId(parentId);
            d.setDeptDeep(depth);
            boolean b = deptService.updateById(d);
            if (!b) {
                throw new CommonException(UpmManagementMessage.DATA_UPDATE_CONFLICT);
            }
            for (Dept child : descendants) {
                if (!deptService.updateById(child)) {
                    throw new CommonException(UpmManagementMessage.DATA_UPDATE_CONFLICT);
                }
                deptService.cleanCache(child.getId());
            }
        } else {
            throw new CommonException(UpmManagementMessage.DATA_NOT_FOUND);
        }
    }

    public void deleteById(Long id) {
        Dept dept = deptService.getById(id);
        if (dept != null) {
            if (CommonConst.NO.equals(dept.getOperable())) {
                throw new CommonException(UpmManagementMessage.DATA_NOT_OPERABLE);
            }
            List<User> userList = userService.findByDeptId(id);
            if (CollUtil.isNotEmpty(userList)) {
                throw new CommonException(UpmManagementMessage.DEPT_HAS_USERS);
            }
            List<Dept> deptList = deptService.findByParentId(dept.getId(), null);
            if (CollUtil.isNotEmpty(deptList)) {
                throw new CommonException(UpmManagementMessage.DEPT_HAS_CHILDREN);
            }
            if (!deptLeaderService.findCurrentByDeptId(id).isEmpty()) {
                throw new CommonException(UpmManagementMessage.DEPT_HAS_LEADERS);
            }
            commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), id);
        } else {
            throw new CommonException(UpmManagementMessage.DATA_ID_NOT_FOUND);
        }
        deptService.removeById(id);
        deptService.cleanCache(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void sortOrder(SortMoveRequest dto) {
        Long fId = dto.getFId();
        Long sId = dto.getSId();
        List<Dept> deptList = deptService.findByIdList(ListUtil.of(fId, sId));
        if (deptList.size() != 2) {
            throw new CommonException(UpmManagementMessage.DATA_NOT_FOUND);
        }
        Dept fDept = deptList.get(0);
        Dept sDept = deptList.get(1);
        if (CommonConst.NO.equals(fDept.getOperable()) || CommonConst.NO.equals(sDept.getOperable())) {
            throw new CommonException(UpmManagementMessage.DATA_NOT_OPERABLE);
        }
        if (!fDept.getParentId().equals(sDept.getParentId())) {
            throw new CommonException(UpmManagementMessage.DEPT_DIFFERENT_PARENT);
        }
        commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), fDept.getId());
        commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), sDept.getId());
        Integer fSortNumber = fDept.getSortNumber();
        fDept.setSortNumber(sDept.getSortNumber());
        sDept.setSortNumber(fSortNumber);
        if (!deptService.updateById(fDept) || !deptService.updateById(sDept)) {
            throw new CommonException(UpmManagementMessage.DATA_SORT_CONFLICT);
        }
        deptService.cleanCache(fId);
        deptService.cleanCache(sId);
    }

    /** 新增或移动时校验目标父部门；编辑未移动的部门不要求拥有其上级部门的数据范围。 */
    private Dept checkParent(Long deptId, Long parentId, boolean checkScope) {
        if (parentId == null || parentId < ROOT_PARENT_ID) {
            throw new CommonException(UpmManagementMessage.DEPT_PARENT_INVALID);
        }
        if (parentId == ROOT_PARENT_ID) {
            // 顶层不是实际部门ID，只允许具有全部部门数据范围的管理用户新增或迁入。
            if (checkScope && commonHandler.findDataScopeByUserId(StpUtil.getLoginIdAsLong(), null) != null) {
                throw new CommonException(UpmManagementMessage.DATA_FORBIDDEN);
            }
            return null;
        }
        Set<Long> visited = new HashSet<>();
        Long current = parentId;
        Dept parent = null;
        while (current != null && current != ROOT_PARENT_ID) {
            if (Objects.equals(current, deptId) || !visited.add(current)) {
                throw new CommonException(UpmManagementMessage.DEPT_PARENT_CYCLE);
            }
            Dept ancestor = deptService.getById(current);
            if (ancestor == null || ancestor.getDeptDeep() == null) {
                throw new CommonException(UpmManagementMessage.DEPT_PARENT_INVALID);
            }
            if (parent == null) {
                parent = ancestor;
            }
            current = ancestor.getParentId();
        }
        if (checkScope) {
            commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), parentId);
        }
        return parent;
    }

    /** 包含停用部门，避免仅检查启用子部门而漏掉隐藏分支或循环关系。 */
    private List<Dept> descendantUpdates(Long rootId, int rootDepth) {
        List<Dept> result = new ArrayList<>();
        ArrayDeque<Dept> pending = new ArrayDeque<>();
        Dept root = new Dept();
        root.setId(rootId);
        root.setDeptDeep(rootDepth);
        pending.add(root);
        Set<Long> visited = new HashSet<>();
        visited.add(rootId);
        while (!pending.isEmpty()) {
            Dept parent = pending.removeFirst();
            for (Dept child : deptService.findByParentId(parent.getId(), null)) {
                if (!visited.add(child.getId())) {
                    throw new CommonException(UpmManagementMessage.DEPT_PARENT_CYCLE);
                }
                commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), child.getId());
                if (CommonConst.NO.equals(child.getOperable())) {
                    throw new CommonException(UpmManagementMessage.DATA_NOT_OPERABLE);
                }
                Dept update = new Dept();
                update.setId(child.getId());
                update.setVersion(child.getVersion());
                update.setDeptDeep(parent.getDeptDeep() + 1);
                result.add(update);
                pending.add(update);
            }
        }
        return result;
    }
}
