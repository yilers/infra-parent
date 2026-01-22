package io.github.yilers.core.constant;

/**
 * 公共常量
 *
 * @author zhanghui
 * @since 2023/9/11 10:14
 */

public class CommonConst {

    /**
     * 1-是 0-否
     */
    public static final Integer YES = 1;
    /**
     * 否
     */
    public static final Integer NO = 0;

    /**
     * 权限类型 1-菜单 2-按钮
     */
    public static final Integer MENU = 1;
    /**
     * 按钮
     */
    public static final Integer BUTTON = 2;

    /**
     * 内部调用
     */
    public static final String INNER = "inner";

    /**
     * Tenant-Id
     */
    public static final String HEADER_TENANT_ID = "Tenant-Id";
    /**
     * Device
     */
    public static final String HEADER_DEVICE = "Device";
    /**
     * User-Id
     */
    public static final String HEADER_USER_ID = "User-Id";

    /**
     * 平台管理员
     */
    public static final String PLATFORM_ADMIN_ROLE_NAME = "平台管理员";

    /**
     * 平台管理员角色编码
     */
    public static final String PLATFORM_ADMIN_ROLE_CODE = "platformAdmin";

    /**
     * 租户管理员角色编码
     */
    public static final String TENANT_ADMIN_ROLE_CODE = "tenantAdmin";

    /**
     * 部门管理员角色编码
     */
    public static final String DEPT_ADMIN_ROLE_CODE = "deptAdmin";

    /**
     * 角色ID与权限编码缓存key
     */
    public static final String ROLE_PERMISSION_CACHE_KEY = "role:roleId2PermissionCode:{}";

    /**
     * 用户ID与角色ID缓存key
     */
    public static final String USER_ROLE_ID_CACHE_KEY = "user:userId2RoleIdList:{}";

    /**
     * 用户ID与角色编码缓存key
     */
    public static final String USER_ROLE_CODE_CACHE_KEY = "user:userId2RoleCodeList:{}";

    /**
     * 缓存key有效期
     */
    public static final Long KEY_EXPIRE = 60 * 60 * 24 * 5L;

    /**
     * 初始化密码
     */
    public static final String INIT_PWD = "yilers@123";

}
