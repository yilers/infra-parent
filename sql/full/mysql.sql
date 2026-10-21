-- MySQL 5.7 / 8.x 全量初始化：仅在空数据库执行一次。
-- 已包含截至2026-09-07的应用改造，不要再执行同版本增量迁移。
-- 执行前阅读 sql/README.md；已有数据库请使用 sql/migration/。
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


INSERT INTO `upm_dept` (`id`, `parent_id`, `dept_code`, `dept_name`, `dept_desc`, `sort_number`, `dept_deep`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`) VALUES (10, 0, 'yilers', 'yilers', NULL, 1, 1, 1, 1, 0, 1, 1, '2026-01-08 15:42:17.808', '2026-01-08 15:42:17.808');



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

INSERT INTO `upm_position` (`id`, `position_code`, `position_name`, `position_desc`, `sort_number`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`) VALUES (10, 'dev', '开发', NULL, 1, 1, 1, 0, 1, 1, '2025-06-05 10:50:30.000', '2025-06-05 10:50:32.000');
INSERT INTO `upm_position` (`id`, `position_code`, `position_name`, `position_desc`, `sort_number`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`) VALUES (20, 'user', '员工', NULL, 2, 1, 1, 0, 1, 1, '2025-06-05 11:07:01.740', '2025-06-05 11:07:01.740');

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

INSERT INTO `upm_user` (`id`, `user_type`, `account`, `nickname`, `password`, `dept_id`, `position_id`, `name`, `gender`, `photo`, `id_card`, `email`, `phone`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_id`, `expand`, `create_time`, `update_time`) VALUES (10, 'admin', 'platform@yilers.com', '超级管理员', '$2a$10$1FylLzVNqSTschwjVyADUOcDplWdLJk2cF1N/5pd8yrep9O2RTVQa', 10, 10, '超级管理员', 1, NULL, NULL, NULL, NULL, 1, 1, 0, 1, 1, 1, '{\"initPwd\":true}', '2025-06-05 10:42:24.565', '2026-01-23 15:52:13.989');
INSERT INTO `upm_user` (`id`, `user_type`, `account`, `nickname`, `password`, `dept_id`, `position_id`, `name`, `gender`, `photo`, `id_card`, `email`, `phone`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_id`, `expand`, `create_time`, `update_time`) VALUES (20, 'admin', 'admin@yilers.com', '租户管理员', '$2a$10$1FylLzVNqSTschwjVyADUOcDplWdLJk2cF1N/5pd8yrep9O2RTVQa', 10, 10, '租户管理员', 1, '', '', '', '', 1, 1, 0, 1, 23, 10, '{\"initPwd\":true}', '2025-06-23 17:59:34.427', '2026-01-23 15:54:59.776');

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
CREATE TABLE upm_application (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64) NOT NULL,
    icon VARCHAR(255) DEFAULT '',
    logo VARCHAR(255) DEFAULT '',
    home_url VARCHAR(500) DEFAULT '',
    sso_enabled TINYINT NOT NULL DEFAULT 0,
    sso_secret VARCHAR(512) DEFAULT NULL,
    redirect_uris TEXT,
    sso_push_url VARCHAR(500) DEFAULT '',
    secret_update_time DATETIME(3) DEFAULT NULL,
    description VARCHAR(500) DEFAULT '',
    sort_number INT NOT NULL DEFAULT 0,
    operable SMALLINT NOT NULL DEFAULT 1,
    usable SMALLINT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    create_id BIGINT,
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_application_tenant_code UNIQUE (tenant_id, code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE upm_permission (
    app_id BIGINT NOT NULL,
    id BIGINT NOT NULL COMMENT '主键ID',
    source_id BIGINT DEFAULT NULL COMMENT '租户1模板菜单ID',
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
    INDEX idx_parent_id (parent_id),
    UNIQUE INDEX uk_permission_tenant_source (tenant_id, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='权限表';

INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (10, 0, 'carbon:align-box-bottom-right', 'system', 1, NULL, '系统管理', 0, 0, 1, 0, 1, 1, '2025-06-05 16:15:23.519', '2025-06-10 11:51:45.730', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (110, 10, 'carbon:user-profile', 'user', 40, '', '用户管理', 1, 0, 1, 0, 1, 3, '2025-06-06 14:58:09.033', '2025-06-20 09:36:09.317', 'system/user/index', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (111, 110, NULL, NULL, 1, 'system:user:list', '列表', 2, 0, 1, 0, 1, 5, '2025-06-06 15:08:56.687', '2025-06-20 09:36:09.687', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (112, 110, NULL, NULL, 2, 'system:user:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:09.818', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (113, 110, NULL, NULL, 3, 'system:user:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:09.937', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (114, 110, NULL, NULL, 4, 'system:user:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.048', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (115, 110, NULL, NULL, 5, 'system:user:dataScope', '数据权限', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.165', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (116, 110, NULL, NULL, 6, 'system:user:updatePwd', '重置密码', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.165', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (117, 110, NULL, NULL, 7, 'system:user:unlock', '解除登录锁定', 2, 1, 1, 0, 1, 1, '2026-09-29 00:00:00.000', '2026-09-29 00:00:00.000', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (118, 110, NULL, NULL, 8, 'system:user:batch:add', '批量新增', 2, 1, 1, 0, 1, 1, '2026-09-30 00:00:00.000', '2026-09-30 00:00:00.000', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (120, 10, 'ic:sharp-menu', 'menu', 20, '', '菜单管理', 1, 0, 1, 0, 1, 1, '2025-06-09 13:35:10.730', '2025-06-20 09:36:10.268', 'system/menu/index', 1, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (121, 120, NULL, NULL, 1, 'system:menu:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:10.402', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (122, 120, NULL, NULL, 2, 'system:menu:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:43.538', '2025-06-20 09:36:10.535', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (123, 120, NULL, NULL, 3, 'system:menu:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:05:08.738', '2025-06-20 09:36:10.676', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (124, 120, NULL, NULL, 4, 'system:menu:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:10.787', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (130, 10, 'mingcute:department-line', 'dept', 50, '', '部门管理', 1, 0, 1, 0, 1, 1, '2025-06-09 14:44:48.238', '2025-06-20 09:36:11.132', 'system/dept/index', 1, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (131, 130, NULL, NULL, 1, 'system:dept:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.239', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (132, 130, NULL, NULL, 2, 'system:dept:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.299', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (133, 130, NULL, NULL, 3, 'system:dept:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.419', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (134, 130, NULL, NULL, 4, 'system:dept:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.584', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, `app_id`) VALUES (196, 130, 5, 'system:dept:leader:edit', '设置主管', 2, 1, 1, 0, 1, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (140, 10, 'carbon:align-vertical-center', 'post', 60, '', '岗位管理', 1, 0, 1, 0, 1, 2, '2025-06-10 10:49:45.307', '2025-06-20 09:36:11.655', 'system/post/index', 1, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (141, 140, NULL, NULL, 1, 'system:position:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.746', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (142, 140, NULL, NULL, 2, 'system:position:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.830', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (143, 140, NULL, NULL, 3, 'system:position:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.908', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (144, 140, NULL, NULL, 4, 'system:position:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.998', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (150, 10, 'carbon:workspace', 'tenant', 10, '', '租户管理', 1, 0, 1, 0, 1, 3, '2025-06-10 15:34:04.749', '2025-06-20 09:36:12.088', 'system/tenant/index', 1, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (151, 150, NULL, NULL, 1, 'system:tenant:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.165', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (152, 150, NULL, NULL, 2, 'system:tenant:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.232', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (153, 150, NULL, NULL, 3, 'system:tenant:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.308', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (154, 150, NULL, NULL, 4, 'system:tenant:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.377', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (195, 150, NULL, 5, 'system:tenant:sync', '同步应用菜单', 2, 1, 1, 0, 1, 1, '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (160, 10, 'carbon:scis-control-tower', 'role', 30, '', '角色管理', 1, 0, 1, 0, 1, 3, '2025-06-10 15:45:32.109', '2025-06-20 09:36:12.466', 'system/role/index', 1, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (161, 160, NULL, NULL, 1, 'system:role:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.586', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (162, 160, NULL, NULL, 2, 'system:role:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.650', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (163, 160, NULL, NULL, 3, 'system:role:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.741', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (164, 160, NULL, NULL, 4, 'system:role:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.868', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (165, 160, NULL, NULL, 5, 'system:role:bindUser', '分配用户', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.944', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (166, 160, NULL, NULL, 6, 'system:role:permission', '授权', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.944', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (170, 10, 'material-symbols:logo-dev-outline', 'log', 70, NULL, '日志管理', 1, 0, 1, 0, 1, 2, '2025-06-16 16:29:04.538', '2025-06-20 09:36:13.033', 'system/log/index', 1, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `create_time`, `update_time`, `component`, `cache`, `link`, `device`, `create_id`, app_id) VALUES (171, 170, NULL, NULL, 1, 'system:log:list', '列表', 2, 0, 1, 0, 1, 6, '2025-06-16 16:29:54.313', '2025-06-20 09:36:13.108', '', 0, 0, 'web', 1, 1);
INSERT INTO `upm_permission` (`id`, `parent_id`, `menu_icon`, `component`, `cache`, `link`, `menu_url`, `sort_number`, `permission_code`, `permission_name`, `permission_type`, `operable`, `usable`, `deleted`, `tenant_id`, `version`, `device`, `create_id`, `create_time`, `update_time`, app_id) VALUES (180, 10, 'carbon:doc', '', 0, 1, 'http://localhost:9003/doc.html', 80, '', '接口文档', 1, 1, 1, 0, 1, 48, 'web', 10, '2025-07-02 15:47:00.771', '2026-01-23 16:05:44.405', 1);

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
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 117, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 118, 1, 'web');
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
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 196, 1, 'web');
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
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 195, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 160, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 161, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 162, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 163, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 164, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 165, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 166, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 170, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 171, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (10, 180, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 10, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 110, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 111, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 112, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 113, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 114, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 115, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 116, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 117, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 118, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 130, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 131, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 132, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 133, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 134, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 196, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 140, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 141, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 142, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 143, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 144, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 160, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 161, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 162, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 163, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 164, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 165, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 166, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 170, 1, 'web');
INSERT INTO `upm_role_permission` (`role_id`, `permission_id`, `tenant_id`, `device`) VALUES (20, 171, 1, 'web');

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
  	platform varchar(30) DEFAULT 'wx' COMMENT '平台',
    open_id varchar(128) DEFAULT NULL COMMENT 'openId',
    union_id varchar(128) DEFAULT NULL COMMENT 'unionId',
    session_key varchar(100) DEFAULT '' COMMENT 'sessionKey',
    expand varchar(1000) DEFAULT '' COMMENT '扩展字段',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_user_third_user_platform (tenant_id, user_id, platform),
    UNIQUE INDEX uk_user_third_open_platform (tenant_id, platform, open_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='用户第三方关联表';

-- 第三方认证平台配置表
CREATE TABLE upm_third_auth_config (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    platform VARCHAR(30) NOT NULL COMMENT '第三方认证平台编码',
    client_id VARCHAR(255) DEFAULT '' COMMENT '第三方平台Client ID或AppKey',
    client_secret VARCHAR(512) DEFAULT '' COMMENT '第三方平台Client Secret或AppSecret',
    redirect_uri VARCHAR(500) DEFAULT '' COMMENT '第三方平台授权回调地址',
    scopes VARCHAR(1000) DEFAULT '' COMMENT '授权范围，多个以英文逗号分隔',
    description VARCHAR(500) DEFAULT '' COMMENT '配置说明',
    operable TINYINT NOT NULL DEFAULT 1 COMMENT '是否可操作 1-是 0-否',
    usable TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用 1-启用 0-停用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除 1-是 0-否',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    create_id BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_third_auth_tenant_platform (tenant_id, platform, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='第三方认证平台配置表';

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

INSERT INTO `upm_tenant` (`id`, `name`, `code`, `description`, `expand`, `operable`, `usable`, `deleted`, `version`, `create_time`, `update_time`) VALUES (1, '默认租户', 'yilers.com', '默认租户', '{\"logo\":\"https://files.authing.co/user-contents/photos/b97119c3-5772-4a2d-804b-b9727c4cd124.png\",\"name\":\"UPM\"}', 0, 1, 0, 1, '2025-06-09 15:48:25', '2026-01-23 15:51:35');

-- 租户账号密码登录策略表（物理删除；无记录表示不限制）
CREATE TABLE upm_tenant_login_policy (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    max_failures INT NOT NULL DEFAULT 3 COMMENT '统计周期内最大连续失败次数',
    failure_window_minutes INT NOT NULL DEFAULT 10 COMMENT '失败统计周期，单位分钟',
    lock_duration_minutes INT NOT NULL DEFAULT 10 COMMENT '账号锁定时长，单位分钟',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_tenant_login_policy_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='租户账号密码登录策略表';

INSERT INTO upm_tenant_login_policy
    (id, tenant_id, max_failures, failure_window_minutes, lock_duration_minutes, version)
VALUES (1, 1, 3, 10, 10, 1);

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
INSERT INTO `upm_device` (`id`, `name`, `code`, `description`, `expand`, `operable`, `usable`, `deleted`, `version`, `tenant_id`, `create_time`, `update_time`) VALUES (20, 'app端', 'app', '', '', 0, 1, 0, 1, 1, '2026-01-23 15:05:36', '2026-01-23 15:05:36');

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
    leader_type    TINYINT NOT NULL DEFAULT 1 COMMENT '主管类型 1-主管 2-副主管',
    remark         VARCHAR(50) DEFAULT '' COMMENT '备注',
    start_date     DATE NOT NULL COMMENT '任职开始日期',
    end_date       DATE DEFAULT NULL COMMENT '任职结束日期（不包含当天）, NULL表示仍在任',
    tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    create_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_dept_leader_dept (tenant_id, dept_id, end_date),
    INDEX idx_dept_leader_user (tenant_id, user_id, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='部门主管配置';

-- 基础管理平台及应用管理初始菜单（仅新库使用）。
INSERT INTO upm_application (id, tenant_id, name, code, icon, description, sort_number, operable, usable, deleted, version)
SELECT id, id, '基础管理平台', 'infra', 'lucide:shield-check', '内置租户、用户、角色、菜单等基础管理功能', 0, 0, 1, 0, 1
FROM upm_tenant;

INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, menu_url, component, menu_icon, sort_number, device, operable, usable, deleted, version)
VALUES (185, 10, 1, 1, '应用管理', 1, 'application', 'system/application/index', 'lucide:app-window', 15, 'web', 1, 1, 0, 1);
INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
VALUES (186, 185, 1, 1, '列表', 2, 'system:application:list', 1, 'web', 1, 1, 0, 1);
INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
VALUES (187, 185, 1, 1, '新增', 2, 'system:application:add', 2, 'web', 1, 1, 0, 1);
INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
VALUES (188, 185, 1, 1, '修改', 2, 'system:application:edit', 3, 'web', 1, 1, 0, 1);
INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
VALUES (189, 185, 1, 1, '启停', 2, 'system:application:usable', 4, 'web', 1, 1, 0, 1);
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 185, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 186, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 187, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 188, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 189, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 185, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 186, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 187, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 188, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 189, 1, 'web');

-- 第三方认证配置菜单及按钮。
INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, menu_url, component, menu_icon, sort_number, device, operable, usable, deleted, version)
VALUES (190, 10, 1, 1, '第三方认证', 1, 'third-auth', 'system/third-auth/index', 'simple-icons:authy', 16, 'web', 1, 1, 0, 1);
INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
VALUES (191, 190, 1, 1, '列表', 2, 'system:thirdAuth:list', 1, 'web', 1, 1, 0, 1);
INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
VALUES (192, 190, 1, 1, '新增', 2, 'system:thirdAuth:add', 2, 'web', 1, 1, 0, 1);
INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
VALUES (193, 190, 1, 1, '修改', 2, 'system:thirdAuth:edit', 3, 'web', 1, 1, 0, 1);
INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
VALUES (194, 190, 1, 1, '启停', 2, 'system:thirdAuth:usable', 4, 'web', 1, 1, 0, 1);
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 190, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 191, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 192, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 193, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 194, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 190, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 191, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 192, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 193, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 194, 1, 'web');
-- Flow 定义管理：流程页面接入后直接显示菜单。
INSERT INTO upm_permission
    (id, parent_id, app_id, tenant_id, permission_name, permission_type, menu_url, menu_icon, component,
     sort_number, device, operable, usable, deleted, version)
VALUES
    (197, 0, 1, 1, 'OA审批', 0, 'flow', 'lucide:workflow', '', 2, 'web', 1, 1, 0, 1),
    (198, 197, 1, 1, '流程定义', 1, 'definition', 'lucide:git-branch', 'flow/definition/index', 1, 'web', 1, 1, 0, 1);
INSERT INTO upm_permission
    (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
     sort_number, device, operable, usable, deleted, version)
VALUES
    (199, 198, 1, 1, '列表', 2, 'flow:definition:list', 1, 'web', 1, 1, 0, 1),
    (200, 198, 1, 1, '新增及复制', 2, 'flow:definition:add', 2, 'web', 1, 1, 0, 1),
    (201, 198, 1, 1, '修改', 2, 'flow:definition:edit', 3, 'web', 1, 1, 0, 1),
    (202, 198, 1, 1, '发布及取消发布', 2, 'flow:definition:publish', 4, 'web', 1, 1, 0, 1),
    (203, 198, 1, 1, '删除', 2, 'flow:definition:delete', 5, 'web', 1, 1, 0, 1);
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device)
SELECT r.id, p.id, 1, 'web' FROM upm_role r CROSS JOIN upm_permission p
WHERE r.id IN (10, 20) AND p.id BETWEEN 197 AND 203;
CREATE INDEX idx_permission_app_device ON upm_permission (tenant_id, app_id, device, parent_id);

-- Warm-Flow v1.8.9 原生流程表；来源：Dromara/warm-flow v1.8.9 官方 MySQL 全量脚本。
-- 仅创建引擎七张 flow_ 表；项目自有扩展表使用 flow_ext_ 前缀。
CREATE TABLE `flow_definition`
(
    `id`              bigint          NOT NULL COMMENT '主键id',
    `flow_code`       varchar(40)     NOT NULL COMMENT '流程编码',
    `flow_name`       varchar(100)    NOT NULL COMMENT '流程名称',
    `model_value`            varchar(40)     NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
    `category`        varchar(100)             DEFAULT NULL COMMENT '流程类别',
    `version`         varchar(20)     NOT NULL COMMENT '流程版本',
    `is_publish`      tinyint(1)      NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_custom`     char(1)                  DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)             DEFAULT NULL COMMENT '审批表单路径',
    `activity_status` tinyint(1)      NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `listener_type`   varchar(100)             DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)             DEFAULT NULL COMMENT '监听器路径',
    `ext`             varchar(500)             DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`     datetime                 DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime                 DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`        char(1)                  DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)              DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程定义表';

CREATE TABLE `flow_node`
(
    `id`              bigint        NOT NULL COMMENT '主键id',
    `node_type`       tinyint(1)      NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `definition_id`   bigint          NOT NULL COMMENT '流程定义id',
    `node_code`       varchar(100)    NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)  DEFAULT NULL COMMENT '流程节点名称',
    `permission_flag` varchar(200)  DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
    `node_ratio`      varchar(200) DEFAULT NULL COMMENT '流程签署比例值',
    `coordinate`      varchar(100)  DEFAULT NULL COMMENT '坐标',
    `any_node_skip`   varchar(100)  DEFAULT NULL COMMENT '任意结点跳转',
    `listener_type`   varchar(100)  DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)  DEFAULT NULL COMMENT '监听器路径',
    `form_custom`     char(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)  DEFAULT NULL COMMENT '审批表单路径',
    `version`         varchar(20)     NOT NULL COMMENT '版本',
    `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             text          COMMENT '节点扩展属性',
    `del_flag`        char(1)       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程节点表';

CREATE TABLE `flow_skip`
(
    `id`             bigint       NOT NULL COMMENT '主键id',
    `definition_id`  bigint          NOT NULL COMMENT '流程定义id',
    `now_node_code`  varchar(100)    NOT NULL COMMENT '当前流程节点的编码',
    `now_node_type`  tinyint(1)   DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `next_node_code` varchar(100)    NOT NULL COMMENT '下一个流程节点的编码',
    `next_node_type` tinyint(1)   DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `skip_name`      varchar(100) DEFAULT NULL COMMENT '跳转名称',
    `skip_type`      varchar(40)  DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
    `skip_condition` varchar(200) DEFAULT NULL COMMENT '跳转条件',
    `coordinate`     varchar(100) DEFAULT NULL COMMENT '坐标',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`       char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`      varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='节点跳转关联表';

CREATE TABLE `flow_instance`
(
    `id`              bigint      NOT NULL COMMENT '主键id',
    `definition_id`   bigint      NOT NULL COMMENT '对应flow_definition表的id',
    `business_id`     varchar(40) NOT NULL COMMENT '业务id',
    `node_type`       tinyint(1)  NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `node_code`       varchar(40) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)         DEFAULT NULL COMMENT '流程节点名称',
    `variable`        text COMMENT '任务变量',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `activity_status` tinyint(1)  NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `def_json`        text COMMENT '流程定义json',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             varchar(500)         DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
    `del_flag`        char(1)              DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)          DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程实例表';

CREATE TABLE `flow_task`
(
    `id`            bigint       NOT NULL COMMENT '主键id',
    `definition_id` bigint       NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`   bigint       NOT NULL COMMENT '对应flow_instance表的id',
    `node_code`     varchar(100) NOT NULL COMMENT '节点编码',
    `node_name`     varchar(100) DEFAULT NULL COMMENT '节点名称',
    `node_type`     tinyint(1)   NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`   char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`     varchar(100) DEFAULT NULL COMMENT '审批表单路径',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='待办任务表';

CREATE TABLE `flow_his_task`
(
    `id`               bigint(20)                   NOT NULL COMMENT '主键id',
    `definition_id`    bigint(20)                   NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`      bigint(20)                   NOT NULL COMMENT '对应flow_instance表的id',
    `task_id`          bigint(20)                   NOT NULL COMMENT '对应flow_task表的id',
    `node_code`        varchar(100)                 DEFAULT NULL COMMENT '开始节点编码',
    `node_name`        varchar(100)                 DEFAULT NULL COMMENT '开始节点名称',
    `node_type`        tinyint(1)                   DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `target_node_code` varchar(200)                 DEFAULT NULL COMMENT '目标节点编码',
    `target_node_name` varchar(200)                 DEFAULT NULL COMMENT '结束节点名称',
    `approver`         varchar(40)                  DEFAULT NULL COMMENT '审批人',
    `cooperate_type`   tinyint(1)                   NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
    `collaborator`     varchar(500)                  DEFAULT NULL COMMENT '协作人',
    `skip_type`        varchar(10)                  NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
    `flow_status`      varchar(20)                  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`      char(1)                      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`        varchar(100)                 DEFAULT NULL COMMENT '审批表单路径',
    `message`          varchar(500)                 DEFAULT NULL COMMENT '审批意见',
    `variable`         TEXT                         DEFAULT NULL COMMENT '任务变量',
    `ext`              TEXT                         DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`      datetime                     DEFAULT NULL COMMENT '任务开始时间',
    `update_time`      datetime                     DEFAULT NULL COMMENT '审批完成时间',
    `del_flag`         char(1)                      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`        varchar(40)                  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT ='历史任务记录表';


CREATE TABLE `flow_user`
(
    `id`           bigint      NOT NULL COMMENT '主键id',
    `type`         char(1)         NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
    `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
    `associated`   bigint          NOT NULL COMMENT '任务表id',
    `create_time`  datetime    DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `del_flag`     char(1)     DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `user_processed_type` (`processed_by`, `type`),
    KEY `user_associated` (`associated`) USING BTREE
) ENGINE = InnoDB COMMENT ='流程用户表';

-- 审批中心第一批：待办、已办、我发起的及查询索引。仅执行一次。
-- 前置：已完成20261016_flow及流程定义菜单迁移；必须停写并备份。
-- 先确认租户1的infra应用下恰有一个menu_url='flow'的OA目录，
-- 且不存在flow/todo/index、flow/done/index、flow/started/index和本脚本索引。
-- ID从当前最大值后连续分配；其他租户通过“同步应用菜单”获取，不改原有记录。
CREATE TEMPORARY TABLE migration_flow_inbox_ids AS
SELECT p.id AS parent_id, p.app_id,
       (SELECT COALESCE(MAX(id), 0) FROM upm_permission) AS base_id
FROM upm_permission p
JOIN upm_application a ON a.id = p.app_id AND a.tenant_id = 1 AND a.code = 'infra' AND a.deleted = 0
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.permission_type = 0 AND p.menu_url = 'flow';

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, menu_url, menu_icon,
 component, sort_number, device, operable, usable, deleted, version)
SELECT base_id + 1, parent_id, app_id, 1, '我的待办', 1, 'todo', 'lucide:inbox', 'flow/todo/index', 2, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, menu_url, menu_icon,
 component, sort_number, device, operable, usable, deleted, version)
SELECT base_id + 2, parent_id, app_id, 1, '我的已办', 1, 'done', 'lucide:circle-check', 'flow/done/index', 3, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, menu_url, menu_icon,
 component, sort_number, device, operable, usable, deleted, version)
SELECT base_id + 3, parent_id, app_id, 1, '我发起的', 1, 'started', 'lucide:send', 'flow/started/index', 4, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 4, base_id + 1, app_id, 1, '查询', 2, 'flow:task:todo', 1, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 5, base_id + 2, app_id, 1, '查询', 2, 'flow:task:done', 1, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 6, base_id + 3, app_id, 1, '查询', 2, 'flow:instance:mine', 1, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids;

INSERT INTO upm_role_permission(role_id, permission_id, tenant_id, device)
SELECT r.id, p.id, 1, 'web'
FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_flow_inbox_ids m
WHERE r.tenant_id = 1 AND r.id IN (10, 20) AND p.id BETWEEN m.base_id + 1 AND m.base_id + 6;

DROP TABLE migration_flow_inbox_ids;

CREATE INDEX idx_flow_instance_mine ON flow_instance(tenant_id, create_by, create_time, id);
CREATE INDEX idx_flow_history_approver ON flow_his_task(tenant_id, approver, node_type, update_time, id);
CREATE INDEX idx_flow_history_instance ON flow_his_task(tenant_id, instance_id, update_time, id);
CREATE INDEX idx_flow_task_instance ON flow_task(tenant_id, instance_id);
CREATE INDEX idx_flow_user_candidate ON flow_user(tenant_id, associated, type, processed_by);

-- 审批中心第二批：通过、退回按钮。仅执行一次，前置已执行20261019_flow_inbox。
-- 停写、备份，确认模板租户1恰有一个flow/todo/index菜单，
-- 且尚无flow:task:pass、flow:task:reject权限。ID使用现有最大值后连续区间。
CREATE TEMPORARY TABLE migration_flow_task_ids AS
SELECT p.id AS parent_id, p.app_id,
       (SELECT COALESCE(MAX(id), 0) FROM upm_permission) AS base_id
FROM upm_permission p
JOIN upm_application a ON a.id = p.app_id AND a.tenant_id = 1 AND a.code = 'infra' AND a.deleted = 0
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.component = 'flow/todo/index';

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 1, parent_id, app_id, 1, '通过', 2, 'flow:task:pass', 2, 'web', 1, 1, 0, 1 FROM migration_flow_task_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 2, parent_id, app_id, 1, '退回', 2, 'flow:task:reject', 3, 'web', 1, 1, 0, 1 FROM migration_flow_task_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 3, parent_id, app_id, 1, '转办', 2, 'flow:task:transfer', 4, 'web', 1, 1, 0, 1 FROM migration_flow_task_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 4, parent_id, app_id, 1, '委派', 2, 'flow:task:delegate', 5, 'web', 1, 1, 0, 1 FROM migration_flow_task_ids;

INSERT INTO upm_role_permission(role_id, permission_id, tenant_id, device)
SELECT r.id, p.id, 1, 'web'
FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_flow_task_ids m
WHERE r.tenant_id = 1 AND r.id IN (10, 20) AND p.id BETWEEN m.base_id + 1 AND m.base_id + 4;

DROP TABLE migration_flow_task_ids;

-- 请假申请：仅执行一次，前置已执行20261019_flow_inbox和20261019_flow_task_actions。
-- 必须停写、备份；确认尚无flow_ext_leave表、flow/leave/index菜单和flow:leave:*权限，
-- 且模板租户1的infra应用下恰有一个OA审批目录。其他租户随后同步菜单、授权并重新登录。
-- 不创建、复制或修改任何流程定义；提交时使用当前租户已发布的leave流程。
-- 请假示例业务表；与通用引擎分离，不存储重复的审批状态。
CREATE TABLE flow_ext_leave (
    id BIGINT NOT NULL COMMENT '请假申请ID',
    tenant_id VARCHAR(40) NOT NULL COMMENT '租户ID，与Flow引擎保持字符串类型',
    applicant_id VARCHAR(64) NOT NULL COMMENT '申请人用户ID',
    applicant_name VARCHAR(100) NOT NULL COMMENT '申请人姓名快照',
    leave_type VARCHAR(20) NOT NULL COMMENT 'PERSONAL事假 SICK病假 ANNUAL年假 COMP_TIME调休 OTHER其他',
    start_time DATETIME NOT NULL COMMENT '请假开始时间',
    end_time DATETIME NOT NULL COMMENT '请假结束时间',
    duration_hours DECIMAL(8,1) NOT NULL COMMENT '请假小时数，0.5的整数倍，不自动计算排班',
    reason VARCHAR(1000) NOT NULL COMMENT '请假原因，不放入流程变量',
    flow_code VARCHAR(40) NOT NULL COMMENT '绑定流程编码，默认由服务端配置为leave',
    instance_id BIGINT DEFAULT NULL COMMENT '流程实例ID，空值表示草稿',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后编辑或提交时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_leave_instance (tenant_id, instance_id),
    KEY idx_flow_leave_mine (tenant_id, applicant_id, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请业务表';

-- 前四项按流程定义、我的待办、我的已办、我发起的排列；不改变是否可操作和授权状态。
UPDATE upm_permission SET sort_number = CASE component
    WHEN 'flow/definition/index' THEN 1 WHEN 'flow/todo/index' THEN 2
    WHEN 'flow/done/index' THEN 3 WHEN 'flow/started/index' THEN 4 END,
    version = version + 1
WHERE deleted = 0 AND permission_type = 1
  AND component IN ('flow/definition/index', 'flow/todo/index', 'flow/done/index', 'flow/started/index')
  AND EXISTS (SELECT 1 FROM upm_application a
              WHERE a.id = upm_permission.app_id AND a.tenant_id = upm_permission.tenant_id
                AND a.code = 'infra' AND a.deleted = 0);

-- 菜单ID使用当前最大值后的连续6个ID；每条INSERT只读取一次临时表，兼容MySQL 5.7。
CREATE TEMPORARY TABLE migration_flow_leave_ids AS
SELECT p.id AS parent_id, p.app_id,
       (SELECT COALESCE(MAX(id), 0) FROM upm_permission) AS base_id
FROM upm_permission p
JOIN upm_application a ON a.id = p.app_id AND a.tenant_id = 1 AND a.code = 'infra' AND a.deleted = 0
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.permission_type = 0 AND p.menu_url = 'flow';

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,sort_number,device,operable,usable,deleted,version)
SELECT base_id+1,parent_id,app_id,1,'请假申请',1,'leave','lucide:calendar-days','flow/leave/index',5,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+2,base_id+1,app_id,1,'查询',2,'flow:leave:list',1,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+3,base_id+1,app_id,1,'新增',2,'flow:leave:add',2,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+4,base_id+1,app_id,1,'编辑草稿',2,'flow:leave:edit',3,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+5,base_id+1,app_id,1,'提交审批',2,'flow:leave:submit',4,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+6,base_id+1,app_id,1,'删除草稿',2,'flow:leave:delete',5,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_role_permission(role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,1,'web'
FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_flow_leave_ids m
WHERE r.tenant_id=1 AND r.id IN (10,20) AND p.id BETWEEN m.base_id+1 AND m.base_id+6;
DROP TABLE migration_flow_leave_ids;

-- 自动抄送：新库全量初始化。后续升级的已有库执行20261020_flow_cc.mysql.sql。
CREATE TABLE flow_cc (
    id BIGINT NOT NULL COMMENT '雪花ID',
    tenant_id VARCHAR(40) NOT NULL COMMENT '租户ID，与Warm-Flow原生表一致',
    instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    source_task_id BIGINT NOT NULL COMMENT '触发抄送的审批任务ID',
    node_code VARCHAR(100) NOT NULL COMMENT '触发节点编码快照',
    node_name VARCHAR(200) NOT NULL COMMENT '触发节点名称快照',
    recipient_id VARCHAR(40) NOT NULL COMMENT '实际接收用户ID',
    recipient_name VARCHAR(128) NOT NULL COMMENT '实际接收用户姓名快照',
    create_time DATETIME(3) NOT NULL COMMENT '抄送时间',
    read_time DATETIME(3) DEFAULT NULL COMMENT '首次查看时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_cc_task_recipient (tenant_id, source_task_id, recipient_id),
    KEY idx_flow_cc_inbox (tenant_id, recipient_id, create_time, id),
    KEY idx_flow_cc_instance (tenant_id, instance_id)
) COMMENT='流程节点自动抄送接收记录';

CREATE TEMPORARY TABLE migration_flow_cc_ids AS
SELECT p.id AS parent_id, p.app_id,
       (SELECT COALESCE(MAX(id), 0) FROM upm_permission) AS base_id
FROM upm_permission p
JOIN upm_application a ON a.id=p.app_id AND a.tenant_id=1 AND a.code='infra' AND a.deleted=0
WHERE p.tenant_id=1 AND p.deleted=0 AND p.permission_type=0 AND p.menu_url='flow';
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,sort_number,device,operable,usable,deleted,version)
SELECT base_id+1,parent_id,app_id,1,'抄送我的',1,'cc','lucide:mail-check','flow/cc/index',5,'web',1,1,0,1 FROM migration_flow_cc_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+2,base_id+1,app_id,1,'查询',2,'flow:cc:list',1,'web',1,1,0,1 FROM migration_flow_cc_ids;
INSERT INTO upm_role_permission(role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,1,'web' FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_flow_cc_ids m
WHERE r.tenant_id=1 AND r.id IN (10,20) AND p.id BETWEEN m.base_id+1 AND m.base_id+2;
UPDATE upm_permission p SET sort_number=6
WHERE p.tenant_id=1 AND p.deleted=0 AND p.component='flow/leave/index'
  AND EXISTS (SELECT 1 FROM migration_flow_cc_ids m
              WHERE p.parent_id=m.parent_id AND p.app_id=m.app_id);
DROP TABLE migration_flow_cc_ids;

-- 申请人拿回和撤销按钮；已有数据库升级执行20261021_flow_instance_actions.mysql.sql。
CREATE TEMPORARY TABLE migration_flow_instance_action_ids AS
SELECT p.id AS parent_id,p.app_id,(SELECT COALESCE(MAX(id),0) FROM upm_permission) AS base_id
FROM upm_permission p
JOIN upm_application a ON a.id=p.app_id AND a.tenant_id=1 AND a.code='infra' AND a.deleted=0
WHERE p.tenant_id=1 AND p.deleted=0 AND p.component='flow/started/index';
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+1,parent_id,app_id,1,'拿回',2,'flow:instance:take-back',2,'web',1,1,0,1
FROM migration_flow_instance_action_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+2,parent_id,app_id,1,'撤销',2,'flow:instance:revoke',3,'web',1,1,0,1
FROM migration_flow_instance_action_ids;
INSERT INTO upm_role_permission(role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,1,'web' FROM upm_role r CROSS JOIN upm_permission p
CROSS JOIN migration_flow_instance_action_ids m
WHERE r.tenant_id=1 AND r.id IN (10,20) AND p.id BETWEEN m.base_id+1 AND m.base_id+2;
DROP TABLE migration_flow_instance_action_ids;
