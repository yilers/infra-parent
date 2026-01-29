package io.github.yilers.upm.handler;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.core.collection.CollUtil;
import cn.hutool.v7.core.util.ObjUtil;
import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.crypto.digest.BCrypt;
import cn.hutool.v7.extra.spring.cglib.CglibUtil;
import cn.hutool.v7.json.JSONUtil;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.core.enums.UserTypeEnum;
import io.github.yilers.upm.dto.UserExpandDTO;
import io.github.yilers.upm.entity.Permission;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.request.UserPageRequest;
import io.github.yilers.upm.request.UserRequest;
import io.github.yilers.upm.request.UserUpdatePwdRequest;
import io.github.yilers.upm.response.UserInfoResponse;
import io.github.yilers.upm.service.RolePermissionService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.base.BaseOperateRequest;
import io.github.yilers.web.base.BasePageRequest;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHandler {
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;
    private final CommonHandler commonHandler;
    private final AuthHandler authHandler;


    public UserInfoResponse currentInfo() {
        try {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            long userId = StpUtil.getLoginIdAsLong();
            UserInfoResponse userInfo = userService.currentInfo(userId);
            List<Role> roleList = userRoleService.findRoleListByUserId(userId);
            userInfo.setRoleList(roleList);
            if (CollUtil.isNotEmpty(roleList)) {
                List<Long> roleIdList = roleList.stream().map(Role::getId).collect(Collectors.toList());
                List<Permission> permissionList = rolePermissionService.findPermissionListByRoleIdList(roleIdList);
                userInfo.setPermissionList(permissionList);
            }
            return userInfo;
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }

    }

    /**
     * 查询用户数据权限
     * null为全部权限 其他返回部门ids
     * @param userId
     * @return
     */
    public List<Long> findDataScopeByUserId(Long userId) {
        return commonHandler.findDataScopeByUserId(userId, null);
    }

    /**
     * 新增用户
     * @param request
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addUser(UserRequest request) {
        String account = request.getAccount();
        User user = userService.findByAccount(account);
        if (ObjUtil.isNotEmpty(user)) {
            throw new CommonException("账号已存在");
        }
        commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), request.getDeptId());
        User addUser = CglibUtil.copy(request, User.class);
        addUser.setUserType(UserTypeEnum.ADMIN.getCode());
        // md5的数据
        String password = request.getPassword();
        String hashpw = BCrypt.hashpw(password);
        addUser.setPassword(hashpw);
        UserExpandDTO userExpandDTO = new UserExpandDTO();
        userExpandDTO.setInitPwd(Boolean.TRUE);
        addUser.setExpand(JSONUtil.toJsonStr(userExpandDTO));
        boolean save = userService.save(addUser);
        if (save) {
            userRoleService.saveUserRoleRelation(addUser.getId(), request.getRoleIdList());
        }
        return addUser.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserRequest request) {
        Long id = request.getId();
        User oldUser = userService.getById(id);
        String account = request.getAccount();
        User user = userService.findByAccount(account);
        if (CommonConst.NO.equals(user.getOperable())) {
            throw new CommonException("数据不可操作");
        }
        if (ObjUtil.isNotEmpty(user) && !user.getId().equals(id)) {
            throw new CommonException("账号已存在");
        }
        commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), user.getDeptId());
        CglibUtil.copy(request, oldUser);
        if (StrUtil.isNotBlank(request.getPassword())) {
            // md5的数据
            String password = request.getPassword();
            String hashpw = BCrypt.hashpw(password);
            oldUser.setPassword(hashpw);
        }
        boolean b = userService.updateById(oldUser);
        if (b) {
            userRoleService.deleteByUserId(id);
            // 缓存用户角色删除
            String key = StrUtil.format(CommonConst.USER_ROLE_ID_CACHE_KEY, id);
            SaManager.getSaTokenDao().delete(key);
            key = StrUtil.format(CommonConst.USER_ROLE_CODE_CACHE_KEY, id);
            SaManager.getSaTokenDao().delete(key);
            userRoleService.saveUserRoleRelation(id, request.getRoleIdList());
            // 清理当前人缓存
            userService.cleanCache(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = "user:", key = "#dto.id")
    @CacheInvalidate(name = "user:currentInfo:", key = "#dto.id")
    public void usable(BaseOperateRequest dto) {
        Long id = dto.getId();
        User user = userService.getById(id);
        if (CommonConst.NO.equals(user.getOperable())) {
            throw new CommonException("数据不可操作");
        }
        commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), user.getDeptId());
        if (id.equals(StpUtil.getLoginIdAsLong())) {
            throw new CommonException("不能操作自己");
        }
        // 可用状态切换为不可用 需要立即下线
        if (user.getUsable().equals(CommonConst.YES)) {
            authHandler.logout(id);
        }
        // 切换可用状态
        user.setUsable(user.getUsable().equals(CommonConst.YES) ? CommonConst.NO : CommonConst.YES);
        user.setUpdateTime(LocalDateTime.now());
        user.updateById();
    }

    public Page<UserInfoResponse> page(BasePageRequest<UserPageRequest> request) {
        // 判断当前人有没有平台角色 没有则不能看到平台用户
        List<String> roleList = StpUtil.getRoleList(StpUtil.getLoginId());
        if (CollUtil.contains(roleList, CommonConst.PLATFORM_ADMIN_ROLE_CODE)) {
            request.getData().setLookPlatformUser(CommonConst.YES);
        }
        Page<?> p = new Page<>(request.getCurrent(), request.getSize());
        Page<UserInfoResponse> page = userService.findByPage(p, request);
        List<UserInfoResponse> records = page.getRecords();
        List<Long> userIdList = new ArrayList<>();
        for (UserInfoResponse record : records) {
            userIdList.add(record.getId());
        }
        if (CollUtil.isNotEmpty(userIdList)) {
            Map<Long, List<Role>> map = userRoleService.findRoleByUserIdList(userIdList);
            for (UserInfoResponse record : records) {
                record.setRoleList(map.get(record.getId()));
            }
        }
        return page;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long userId) {
        User user = userService.getById(userId);
        if (CommonConst.NO.equals(user.getOperable())) {
            throw new CommonException("数据不可操作");
        }
        commonHandler.checkDataScope(StpUtil.getLoginIdAsLong(), user.getDeptId());
        // 判断是不是自己 自己不能删除自己
        if (userId.equals(StpUtil.getLoginIdAsLong())) {
            throw new CommonException("不能删除自己");
        }
        // 删除缓存
        String key = StrUtil.format(CommonConst.USER_ROLE_ID_CACHE_KEY, userId);
        SaManager.getSaTokenDao().delete(key);
        key = StrUtil.format(CommonConst.USER_ROLE_CODE_CACHE_KEY, userId);
        SaManager.getSaTokenDao().delete(key);
        userService.removeById(userId);
        userRoleService.deleteByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePwd(UserUpdatePwdRequest request) {
        Long userId = request.getUserId();
        User user = userService.getById(userId);
        if (CommonConst.NO.equals(user.getOperable())) {
            throw new CommonException("数据不可操作");
        }
        String oldPwd = request.getOldPwd();
        String newPwd = request.getNewPwd();
        if (!BCrypt.checkpw(oldPwd, user.getPassword())) {
            throw new CommonException("旧密码错误");
        }
        String hashpw = BCrypt.hashpw(newPwd);
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(hashpw);
        // 设置不为初始密码了
        UserExpandDTO userExpandDTO = new UserExpandDTO();
        userExpandDTO.setInitPwd(Boolean.FALSE);
        updateUser.setExpand(JSONUtil.toJsonStr(userExpandDTO));
        userService.updateById(updateUser);
    }
}
