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
     * 用户基本信息缓存名称
     */
    public static final String USER_CACHE_NAME = "user:";

    /**
     * 当前用户详细信息缓存名称
     */
    public static final String USER_CURRENT_INFO_CACHE_NAME = "user:currentInfo:";

    /**
     * 用户角色缓存名称
     */
    public static final String USER_ROLE_CACHE_NAME = "userRole:";

    /**
     * 用户数据权限覆盖配置缓存名称
     */
    public static final String USER_DATA_SCOPE_CACHE_NAME = "userDataScope:";

    /**
     * 第三方账号绑定OAuth2 state缓存名称
     */
    public static final String THIRD_AUTH_BIND_STATE_CACHE_NAME = "thirdAuth:bindState:";

    /**
     * 部门缓存名称
     */
    public static final String DEPT_CACHE_NAME = "dept:";

    /**
     * 初始化密码
     */
    public static final String INIT_PWD = "yilers@123";

}
