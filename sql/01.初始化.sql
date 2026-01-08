CREATE DATABASE IF NOT EXISTS upm
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_bin;

-- 部门表
CREATE TABLE upm_dept (
    id BIGINT NOT NULL COMMENT '主键ID',
    parent_id BIGINT COMMENT '父id',
    dept_code VARCHAR(30) NOT NULL COMMENT '部门编码',
    dept_name VARCHAR(30) NOT NULL COMMENT '部门名称',
    dept_desc VARCHAR(50) DEFAULT '' COMMENT '部门描述',
    sort_number INT COMMENT '排序',
    dept_deep INT COMMENT '深度',
    operable TINYINT DEFAULT 1 COMMENT '是否可操作 1-是 0-否',
    usable TINYINT DEFAULT 1 COMMENT '是否可用 1-启用 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    version int DEFAULT 1 COMMENT '版本号',
    create_id BIGINT DEFAULT 1 COMMENT '创建人id',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_dept_code (dept_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='部门表';


INSERT INTO `upm_dept` (`id`, `parent_id`, `dept_code`, `dept_name`, `dept_desc`, `sort_number`, `dept_deep`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`) VALUES (10, 0, 'yilers', 'yilers', NULL, 1, 1, 0, 1, 0, 1, 1, '2026-01-08 15:42:17.808', '2026-01-08 15:42:17.808');



-- 职位表
CREATE TABLE upm_position (
    id BIGINT NOT NULL COMMENT '主键ID',
    position_code VARCHAR(30) NOT NULL COMMENT '职位编码',
    position_name VARCHAR(30) NOT NULL COMMENT '职位名称',
    position_desc VARCHAR(50) COMMENT '职位描述',
    sort_number INT COMMENT '排序',
    operable TINYINT DEFAULT 1 COMMENT '是否可操作 1-是 0-否',
    usable TINYINT DEFAULT 1 COMMENT '是否可用 1-启用 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    version int DEFAULT 1 COMMENT '版本号',
    create_id BIGINT DEFAULT 1 COMMENT '创建人id',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (id),
    INDEX idx_position_code (position_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='职位表';

INSERT INTO `upm_position` (`id`, `position_code`, `position_name`, `position_desc`, `sort_number`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`) VALUES (10, 'dev', '开发', NULL, 1, 0, 1, 0, 1, 1, '2025-06-05 10:50:30.000', '2025-06-05 10:50:32.000');
INSERT INTO `upm_position` (`id`, `position_code`, `position_name`, `position_desc`, `sort_number`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`) VALUES (20, 'user', '员工', NULL, 2, 0, 1, 0, 1, 1, '2025-06-05 11:07:01.740', '2025-06-05 11:07:01.740');

-- 用户表
CREATE TABLE upm_user (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_type VARCHAR(20) DEFAULT 'admin' COMMENT 'admin / user',
    account VARCHAR(50) DEFAULT '' COMMENT '账号',
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
    password VARCHAR(100) DEFAULT '' COMMENT '密码',
    dept_id BIGINT COMMENT '所属部门',
    position_id BIGINT COMMENT '所属职位',
    name VARCHAR(50) DEFAULT '' COMMENT '姓名',
    gender TINYINT COMMENT '性别 1-男 2-女',
    photo VARCHAR(255) DEFAULT '' COMMENT '头像',
    id_card VARCHAR(20) DEFAULT '' COMMENT '身份证编码',
    email VARCHAR(30) DEFAULT '' COMMENT '邮箱',
    phone VARCHAR(20)  DEFAULT '' COMMENT '手机',
    operable TINYINT DEFAULT 1 COMMENT '是否可操作 1-是 0-否',
    usable TINYINT DEFAULT 1 COMMENT '是否可用 1-启用 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    version int DEFAULT 1 COMMENT '版本号',
    create_id BIGINT DEFAULT 1 COMMENT '创建人id',
    expand VARCHAR(200) DEFAULT '' COMMENT '扩展字段',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_account (account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户表';

INSERT INTO `upm_user` (`id`, `user_type`, `account`, `nickname`, `password`, `dept_id`, `position_id`, `name`, `gender`, `photo`, `id_card`, `email`, `phone`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`) VALUES (10, 'admin', 'platform@jzy.com', '超级管理员', '$2a$10$JarAnbZXpw85Af1unN3RR.O.L1PgxmO5iEY0MbRuX3/Tj3pIHvyEy', 10, 10, '超级管理员', 1, NULL, NULL, NULL, NULL, 0, 1, 0, 1, 1, '2025-06-05 10:42:24.565', '2025-06-05 10:46:31.524');

-- 角色表
CREATE TABLE upm_role (
    id BIGINT NOT NULL COMMENT '主键ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_desc VARCHAR(255) COMMENT '角色描述',
    data_scope TINYINT NOT NULL DEFAULT 1 COMMENT '数据权限类型 1-全部 2-本部门及以下 3-本部门 4-自定义部门',
    operable TINYINT DEFAULT 1 COMMENT '是否可操作 1-是 0-否',
    usable TINYINT DEFAULT 1 COMMENT '是否可用 1-启用 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    expand VARCHAR(500) DEFAULT '' COMMENT '扩展字段',
    version int DEFAULT 1 COMMENT '版本号',
    create_id BIGINT DEFAULT 1 COMMENT '创建人id',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_tenant_id_role_code (tenant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='角色表';

INSERT INTO `upm_role` (`id`, `role_code`, `role_name`, `role_desc`, `data_scope`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `expand`, `create_id`) VALUES (10, 'platformAdmin', '平台管理员', NULL, 1, 0, 1, 0, 1, 1, '2025-06-05 11:01:54.860', '2025-06-20 10:40:01.308', NULL, 1);
INSERT INTO `upm_role` (`id`, `role_code`, `role_name`, `role_desc`, `data_scope`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `expand`, `create_id`) VALUES (20, 'tenantAdmin', '租户管理员', NULL, 2, 0, 1, 0, 1, 3, '2025-06-11 11:39:18.116', '2025-06-20 10:53:07.365', NULL, 1);
INSERT INTO `upm_role` (`id`, `role_code`, `role_name`, `role_desc`, `data_scope`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `expand`, `create_id`) VALUES (30, 'deptAdmin', '部门管理员', NULL, 2, 0, 1, 0, 1, 5, '2025-06-05 11:03:13.851', '2025-06-20 13:49:48.567', NULL, 1);

-- 权限表
CREATE TABLE upm_permission (
    id BIGINT NOT NULL COMMENT '主键ID',
    parent_id BIGINT COMMENT '父id',
    menu_icon VARCHAR(50) DEFAULT '' COMMENT 'icon',
    component VARCHAR(50) DEFAULT '' COMMENT '组件',
    cache TINYINT DEFAULT 0 COMMENT '缓存 1-是 0-否',
    link TINYINT DEFAULT 0 COMMENT '外链 1-是 0-否',
    menu_url VARCHAR(255) DEFAULT '' COMMENT '路由',
    sort_number INT COMMENT '排序',
    permission_code VARCHAR(50) DEFAULT '' COMMENT '权限编码',
    permission_name VARCHAR(100) DEFAULT '' COMMENT '权限名称',
    permission_type TINYINT COMMENT '类型 0-目录 1-菜单 2-按钮',
    operable TINYINT DEFAULT 1 COMMENT '是否可操作 1-是 0-否',
    usable TINYINT DEFAULT 1 COMMENT '是否可用 1-启用 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    version int DEFAULT 1 COMMENT '版本号',
    device VARCHAR(20) DEFAULT '' COMMENT '设备端',
    create_id BIGINT DEFAULT 1 COMMENT '创建人id',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='权限表';

INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (10, 0, 'carbon:align-box-bottom-right', 'system', 1, NULL, '系统管理', 0, 0, 1, 0, 1, 1, '2025-06-05 16:15:23.519', '2025-06-10 11:51:45.730', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (110, 10, 'carbon:user-profile', 'user', 40, '', '用户管理', 1, 0, 1, 0, 1, 3, '2025-06-06 14:58:09.033', '2025-06-20 09:36:09.317', 'system/user/index', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (111, 110, NULL, NULL, 1, 'system:user:list', '列表', 2, 0, 1, 0, 1, 5, '2025-06-06 15:08:56.687', '2025-06-20 09:36:09.687', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (112, 110, NULL, NULL, 2, 'system:user:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:09.818', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (113, 110, NULL, NULL, 3, 'system:user:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:09.937', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (114, 110, NULL, NULL, 4, 'system:user:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.048', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (115, 110, NULL, NULL, 5, 'system:user:dataScope', '数据权限', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.165', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (116, 110, NULL, NULL, 6, 'system:user:updatePwd', '重置密码', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.165', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (120, 10, 'ic:sharp-menu', 'menu', 20, '', '菜单管理', 1, 0, 1, 0, 1, 1, '2025-06-09 13:35:10.730', '2025-06-20 09:36:10.268', 'system/menu/index', 1, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (121, 120, NULL, NULL, 1, 'system:menu:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:10.402', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (122, 120, NULL, NULL, 2, 'system:menu:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:43.538', '2025-06-20 09:36:10.535', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (123, 120, NULL, NULL, 3, 'system:menu:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:05:08.738', '2025-06-20 09:36:10.676', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (124, 120, NULL, NULL, 4, 'system:menu:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:10.787', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (130, 10, 'mingcute:department-line', 'dept', 50, '', '部门管理', 1, 0, 1, 0, 1, 1, '2025-06-09 14:44:48.238', '2025-06-20 09:36:11.132', 'system/dept/index', 1, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (131, 130, NULL, NULL, 1, 'system:dept:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.239', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (132, 130, NULL, NULL, 2, 'system:dept:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.299', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (133, 130, NULL, NULL, 3, 'system:dept:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.419', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (134, 130, NULL, NULL, 4, 'system:dept:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.584', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (140, 10, 'carbon:align-vertical-center', 'post', 60, '', '岗位管理', 1, 0, 1, 0, 1, 2, '2025-06-10 10:49:45.307', '2025-06-20 09:36:11.655', 'system/post/index', 1, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (141, 140, NULL, NULL, 1, 'system:position:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.746', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (142, 140, NULL, NULL, 2, 'system:position:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.830', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (143, 140, NULL, NULL, 3, 'system:position:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.908', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (144, 140, NULL, NULL, 4, 'system:position:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.998', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (150, 10, 'ph:users-light', 'tenant', 10, '', '租户管理', 1, 0, 1, 0, 1, 3, '2025-06-10 15:34:04.749', '2025-06-20 09:36:12.088', 'system/tenant/index', 1, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (151, 150, NULL, NULL, 1, 'system:tenant:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.165', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (152, 150, NULL, NULL, 2, 'system:tenant:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.232', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (153, 150, NULL, NULL, 3, 'system:tenant:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.308', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (154, 150, NULL, NULL, 4, 'system:tenant:add', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.377', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (160, 10, 'carbon:scis-control-tower', 'role', 30, '', '角色管理', 1, 0, 1, 0, 1, 3, '2025-06-10 15:45:32.109', '2025-06-20 09:36:12.466', 'system/role/index', 1, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (161, 160, NULL, NULL, 1, 'system:role:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.586', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (162, 160, NULL, NULL, 2, 'system:role:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.650', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (163, 160, NULL, NULL, 3, 'system:role:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.741', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (164, 160, NULL, NULL, 4, 'system:role:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.868', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (165, 160, NULL, NULL, 5, 'system:role:bindUser', '分配用户', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.944', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (166, 160, NULL, NULL, 6, 'system:role:permission', '授权', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.944', '', 0, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (170, 10, 'material-symbols:logo-dev-outline', 'log', 70, NULL, '日志管理', 1, 0, 1, 0, 1, 2, '2025-06-16 16:29:04.538', '2025-06-20 09:36:13.033', 'system/log/index', 1, 0, 'web', 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`) VALUES (171, 170, NULL, NULL, 1, 'system:log:list', '列表', 2, 0, 1, 0, 1, 6, '2025-06-16 16:29:54.313', '2025-06-20 09:36:13.108', '', 0, 0, 'web', 1);

-- 角色权限关联表
CREATE TABLE upm_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色id',
    permission_id BIGINT NOT NULL COMMENT '权限id',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    device VARCHAR(20) DEFAULT '' COMMENT '设备端',
    INDEX idx_role_id (role_id),
    UNIQUE INDEX uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='角色权限关联表';

INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 10, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 110, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 111, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 112, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 113, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 114, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 115, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 116, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 120, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 121, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 122, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 123, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 124, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 130, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 131, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 132, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 133, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 134, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 140, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 141, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 142, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 143, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 144, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 150, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 151, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 152, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 153, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 154, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 160, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 161, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 162, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 163, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 164, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 165, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 166, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 170, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 171, 1, 'web');

-- 日志表
CREATE TABLE upm_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    module VARCHAR(50) COMMENT '模块',
    action VARCHAR(50) COMMENT '动作 增删改',
    success TINYINT COMMENT '是否成功 1-是 0-否',
    operator VARCHAR(50) COMMENT '操作人',
    method VARCHAR(255) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    ip VARCHAR(20) COMMENT '请求ip',
    region VARCHAR(30),
    duration int COMMENT '耗时 毫秒',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    dept_id BIGINT COMMENT '部门ID',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    stack_trace TEXT COMMENT '异常堆栈',
    PRIMARY KEY (id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='日志表';

-- 角色部门关联表
CREATE TABLE upm_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    INDEX idx_role_id (role_id),
    UNIQUE INDEX uk_role_dept (role_id, dept_id) COMMENT '角色和部门关联唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='角色部门关联表';

-- 用户角色关联表
CREATE TABLE upm_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    UNIQUE INDEX uk_user_role (user_id, role_id) COMMENT '用户和角色关联唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户角色关联表';

INSERT INTO `upm_user_role` (`user_id`, `role_id`, `tenant_id`) VALUES (10, 10, 1);

-- 角色-表列忽略关联表
CREATE TABLE upm_role_column (
    role_id BIGINT NOT NULL COMMENT '角色ID',
  	table_name varchar(30) DEFAULT '' COMMENT '表名',
    ignore_column varchar(500) DEFAULT '' COMMENT '忽略字段 多个用逗号分隔',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    INDEX idx_role_id (role_id),
    UNIQUE INDEX uk_role_id_table_name (role_id, table_name) COMMENT '角色表名关联唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='角色表列关联表';

-- 用户第三方关联表
CREATE TABLE upm_user_third (
    id BIGINT NOT NULL COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
  	platform varchar(10) DEFAULT 'wx' COMMENT '平台',
    open_id varchar(60) DEFAULT '' COMMENT 'openId',
    union_id varchar(60) DEFAULT '' COMMENT 'unionId',
    session_key varchar(60) DEFAULT '' COMMENT 'sessionKey',
    expand varchar(500) DEFAULT '' COMMENT '扩展字段',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_open_id (open_id) COMMENT '唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户第三方关联表';

-- 租户表
CREATE TABLE upm_tenant (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID，自增',
    name VARCHAR(30) NOT NULL COMMENT '租户名称',
    code VARCHAR(30) NOT NULL COMMENT '租户编码',
    description VARCHAR(500) DEFAULT '' COMMENT '租户描述',
    expand VARCHAR(1000) DEFAULT '' COMMENT '扩展字段',
    operable TINYINT DEFAULT 1 COMMENT '是否可操作 1-是 0-否',
    usable TINYINT DEFAULT 1 COMMENT '是否可用 1-启用 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    version int DEFAULT 1 COMMENT '版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_code (code) COMMENT '租户编码索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='租户表';

INSERT INTO `upm_tenant` (`id`, `name`, `code`, `description`, `expand`, `operable`, `usable`, `deleted`, `version`, `create_time`, `update_time`) VALUES (1, '默认租户', 'jzy.com', '默认租户', '', 0, 1, 0, 1, '2025-06-09 15:48:25', '2025-06-09 15:56:17');

-- 设备表
CREATE TABLE upm_device (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID，自增',
    name VARCHAR(30) NOT NULL COMMENT '名称',
    code VARCHAR(30) NOT NULL COMMENT '编码',
    description VARCHAR(500) DEFAULT '' COMMENT '描述',
    expand VARCHAR(1000) DEFAULT '' COMMENT '扩展字段',
    operable TINYINT DEFAULT 1 COMMENT '是否可操作 1-是 0-否',
    usable TINYINT DEFAULT 1 COMMENT '是否可用 1-启用 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    version int DEFAULT 1 COMMENT '版本号',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_code (code) COMMENT '编码索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='设备表';

INSERT INTO `upm_device` (`id`, `name`, `code`, `description`, `expand`, `operable`, `usable`, `deleted`, `version`, `tenant_id`, `create_time`, `update_time`) VALUES (10, 'web端', 'web', '', '', 0, 1, 0, 1, 1, '2025-06-11 09:13:35', '2025-06-11 09:13:35');

-- 用户仅自己数据权限
CREATE TABLE upm_user_data_scope (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    interface_path VARCHAR(100) NOT NULL COMMENT '接口路径',
    data_scope TINYINT DEFAULT 1 COMMENT '数据权限类型 1-全部 2-本部门及以下 3-本部门 4-自定义部门 5-仅自己',
    expand VARCHAR(500) DEFAULT '' COMMENT '扩展字段',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    INDEX idx_user_id (user_id),
    UNIQUE INDEX uk_user_interface (user_id, interface_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户-数据权限';

CREATE TABLE upm_dept_leader (
    id             BIGINT COMMENT '主键ID',
    dept_id        BIGINT NOT NULL COMMENT '部门ID',
    user_id        BIGINT NOT NULL COMMENT '主管员工ID',
    leader_type    TINYINT NOT NULL DEFAULT 1 COMMENT '主管类型 待定',
    remark         VARCHAR(50) DEFAULT '' COMMENT '备注',
    start_date     DATE NOT NULL COMMENT '任职开始日期',
    end_date       DATE DEFAULT NULL COMMENT '任职结束日期, NULL表示当前任职',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    create_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='部门主管配置';

CREATE TABLE upm_role_column (
    role_id BIGINT NOT NULL COMMENT '角色ID',
  	table_name varchar(30) DEFAULT '' COMMENT '表名',
    ignore_column varchar(500) DEFAULT '' COMMENT '忽略字段 多个用逗号分隔',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    INDEX idx_role_id (role_id),
    UNIQUE INDEX uk_role_id_table_name (role_id, table_name) COMMENT '角色表名关联唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表列关联表';