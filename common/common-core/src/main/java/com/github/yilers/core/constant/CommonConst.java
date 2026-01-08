package com.github.yilers.core.constant;

/**
 * 公共常量
 * @author zhanghui
 * @date 2023/9/11 10:14
 */

public class CommonConst {

    /**
     * 1-是 0-否
     */
    public static final Integer YES = 1;
    public static final Integer NO = 0;

    /**
     * 权限类型 1-菜单 2-按钮
     */
    public static final Integer MENU = 1;
    public static final Integer BUTTON = 2;

    public static final String INNER = "inner";

    public static final String HEADER_TENANT_ID = "Tenant-Id";
    public static final String HEADER_DEVICE = "Device";

    public static final String PLATFORM_ADMIN_ROLE_CODE = "platformAdmin";
    public static final String TENANT_ADMIN_ROLE_CODE = "tenantAdmin";
    public static final String DEPT_ADMIN_ROLE_CODE = "deptAdmin";

    public  static final String ROLE_PERMISSION_CACHE_KEY = "role:roleId2PermissionCode:{}";
    public static final String USER_ROLE_ID_CACHE_KEY = "user:userId2RoleIdList:{}";
    public static final String USER_ROLE_CODE_CACHE_KEY = "user:userId2RoleCodeList:{}";
    public static final Long KEY_EXPIRE = 60*60*24*5L;

    public static final String INIT_PWD = "yilers@123";

}
