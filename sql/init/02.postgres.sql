-- ============================================
-- PostgreSQL 初始化脚本 (UPM 用户权限管理系统)
-- 转换说明：
--   - tenant / device 表: id 自增 (BIGSERIAL)
--   - 其他表: id 由代码雪花算法生成 (BIGINT)
--   - TINYINT → SMALLINT
--   - DATETIME(3) → TIMESTAMP(3)
--   - ON UPDATE 由触发器实现
-- ============================================

-- 部门表
CREATE TABLE upm_dept (
    id BIGINT NOT NULL,
    parent_id BIGINT,
    dept_code VARCHAR(30) NOT NULL,
    dept_name VARCHAR(30) NOT NULL,
    dept_desc VARCHAR(50) DEFAULT '',
    sort_number INT,
    dept_deep INT,
    operable SMALLINT DEFAULT 1,
    usable SMALLINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1,
    version INT DEFAULT 1,
    create_id BIGINT DEFAULT 1,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
CREATE INDEX idx_parent_id ON upm_dept(parent_id);
CREATE INDEX idx_dept_code ON upm_dept(dept_code);
COMMENT ON TABLE upm_dept IS '部门表';
COMMENT ON COLUMN upm_dept.id IS '主键ID';
COMMENT ON COLUMN upm_dept.parent_id IS '父id';
COMMENT ON COLUMN upm_dept.dept_code IS '部门编码';
COMMENT ON COLUMN upm_dept.dept_name IS '部门名称';
COMMENT ON COLUMN upm_dept.dept_desc IS '部门描述';
COMMENT ON COLUMN upm_dept.sort_number IS '排序';
COMMENT ON COLUMN upm_dept.dept_deep IS '深度';
COMMENT ON COLUMN upm_dept.operable IS '是否可操作 1-是 0-否';
COMMENT ON COLUMN upm_dept.usable IS '是否可用 1-启用 0-禁用';
COMMENT ON COLUMN upm_dept.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_dept.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_dept.version IS '版本号';
COMMENT ON COLUMN upm_dept.create_id IS '创建人id';
COMMENT ON COLUMN upm_dept.create_time IS '创建时间';
COMMENT ON COLUMN upm_dept.update_time IS '修改时间';

-- update_time 自动更新触发器（部门表）
CREATE OR REPLACE FUNCTION update_upm_dept_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_dept_update_time
    BEFORE UPDATE ON upm_dept
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_dept_update_time();

INSERT INTO upm_dept (id, parent_id, dept_code, dept_name, dept_desc, sort_number, dept_deep, operable, usable, deleted, tenant_id, version, create_time, update_time)
VALUES (10, 0, 'yilers', 'yilers', NULL, 1, 1, 0, 1, 0, 1, 1, '2026-01-08 15:42:17.808', '2026-01-08 15:42:17.808');


-- 职位表
CREATE TABLE upm_position (
    id BIGINT NOT NULL,
    position_code VARCHAR(30) NOT NULL,
    position_name VARCHAR(30) NOT NULL,
    position_desc VARCHAR(50),
    sort_number INT,
    operable SMALLINT DEFAULT 1,
    usable SMALLINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1,
    version INT DEFAULT 1,
    create_id BIGINT DEFAULT 1,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
CREATE INDEX idx_position_code ON upm_position(position_code);
COMMENT ON TABLE upm_position IS '职位表';
COMMENT ON COLUMN upm_position.id IS '主键ID';
COMMENT ON COLUMN upm_position.position_code IS '职位编码';
COMMENT ON COLUMN upm_position.position_name IS '职位名称';
COMMENT ON COLUMN upm_position.position_desc IS '职位描述';
COMMENT ON COLUMN upm_position.sort_number IS '排序';
COMMENT ON COLUMN upm_position.operable IS '是否可操作 1-是 0-否';
COMMENT ON COLUMN upm_position.usable IS '是否可用 1-启用 0-禁用';
COMMENT ON COLUMN upm_position.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_position.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_position.version IS '版本号';
COMMENT ON COLUMN upm_position.create_id IS '创建人id';
COMMENT ON COLUMN upm_position.create_time IS '创建时间';
COMMENT ON COLUMN upm_position.update_time IS '修改时间';

CREATE OR REPLACE FUNCTION update_upm_position_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_position_update_time
    BEFORE UPDATE ON upm_position
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_position_update_time();

INSERT INTO upm_position (id, position_code, position_name, position_desc, sort_number, operable, usable, deleted, tenant_id, version, create_time, update_time)
VALUES (10, 'dev', '开发', NULL, 1, 0, 1, 0, 1, 1, '2025-06-05 10:50:30.000', '2025-06-05 10:50:32.000');
INSERT INTO upm_position (id, position_code, position_name, position_desc, sort_number, operable, usable, deleted, tenant_id, version, create_time, update_time)
VALUES (20, 'user', '员工', NULL, 2, 0, 1, 0, 1, 1, '2025-06-05 11:07:01.740', '2025-06-05 11:07:01.740');


-- 用户表
CREATE TABLE upm_user (
    id BIGINT NOT NULL,
    user_type VARCHAR(20) DEFAULT 'admin',
    account VARCHAR(50) DEFAULT '',
    nickname VARCHAR(50) DEFAULT '',
    password VARCHAR(100) DEFAULT '',
    dept_id BIGINT,
    position_id BIGINT,
    name VARCHAR(50) DEFAULT '',
    gender SMALLINT,
    photo VARCHAR(255) DEFAULT '',
    id_card VARCHAR(20) DEFAULT '',
    email VARCHAR(30) DEFAULT '',
    phone VARCHAR(20) DEFAULT '',
    operable SMALLINT DEFAULT 1,
    usable SMALLINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1,
    version INT DEFAULT 1,
    create_id BIGINT DEFAULT 1,
    expand VARCHAR(200) DEFAULT '',
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
CREATE INDEX idx_dept_id ON upm_user(dept_id);
CREATE INDEX idx_account ON upm_user(account);
COMMENT ON TABLE upm_user IS '用户表';
COMMENT ON COLUMN upm_user.id IS '主键ID';
COMMENT ON COLUMN upm_user.user_type IS 'admin / user';
COMMENT ON COLUMN upm_user.account IS '账号';
COMMENT ON COLUMN upm_user.nickname IS '昵称';
COMMENT ON COLUMN upm_user.password IS '密码';
COMMENT ON COLUMN upm_user.dept_id IS '所属部门';
COMMENT ON COLUMN upm_user.position_id IS '所属职位';
COMMENT ON COLUMN upm_user.name IS '姓名';
COMMENT ON COLUMN upm_user.gender IS '性别 1-男 2-女';
COMMENT ON COLUMN upm_user.photo IS '头像';
COMMENT ON COLUMN upm_user.id_card IS '身份证编码';
COMMENT ON COLUMN upm_user.email IS '邮箱';
COMMENT ON COLUMN upm_user.phone IS '手机';
COMMENT ON COLUMN upm_user.operable IS '是否可操作 1-是 0-否';
COMMENT ON COLUMN upm_user.usable IS '是否可用 1-启用 0-禁用';
COMMENT ON COLUMN upm_user.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_user.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_user.version IS '版本号';
COMMENT ON COLUMN upm_user.create_id IS '创建人id';
COMMENT ON COLUMN upm_user.expand IS '扩展字段';
COMMENT ON COLUMN upm_user.create_time IS '创建时间';
COMMENT ON COLUMN upm_user.update_time IS '修改时间';

CREATE OR REPLACE FUNCTION update_upm_user_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_user_update_time
    BEFORE UPDATE ON upm_user
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_user_update_time();

INSERT INTO upm_user (id, user_type, account, nickname, password, dept_id, position_id, name, gender, photo, id_card, email, phone, operable, usable, deleted, tenant_id, version, create_id, expand, create_time, update_time)
VALUES (10, 'admin', 'platform@yilers.com', '超级管理员', '$2a$10$1FylLzVNqSTschwjVyADUOcDplWdLJk2cF1N/5pd8yrep9O2RTVQa', 10, 10, '超级管理员', 1, NULL, NULL, NULL, NULL, 1, 1, 0, 1, 1, 1, '{"initPwd":false}', '2025-06-05 10:42:24.565', '2026-01-23 15:52:13.989');
INSERT INTO upm_user (id, user_type, account, nickname, password, dept_id, position_id, name, gender, photo, id_card, email, phone, operable, usable, deleted, tenant_id, version, create_id, expand, create_time, update_time)
VALUES (20, 'admin', 'admin@yilers.com', '租户管理员', '$2a$10$1FylLzVNqSTschwjVyADUOcDplWdLJk2cF1N/5pd8yrep9O2RTVQa', 10, 10, '租户管理员', 1, '', '', '', '', 1, 1, 0, 1, 23, 10, '', '2025-06-23 17:59:34.427', '2026-01-23 15:54:59.776');


-- 角色表
CREATE TABLE upm_role (
    id BIGINT NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_desc VARCHAR(255),
    data_scope SMALLINT NOT NULL DEFAULT 1,
    operable SMALLINT DEFAULT 1,
    usable SMALLINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1,
    expand VARCHAR(500) DEFAULT '',
    version INT DEFAULT 1,
    create_id BIGINT DEFAULT 1,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_id_role_code UNIQUE (tenant_id, role_code)
);
COMMENT ON TABLE upm_role IS '角色表';
COMMENT ON COLUMN upm_role.id IS '主键ID';
COMMENT ON COLUMN upm_role.role_code IS '角色编码';
COMMENT ON COLUMN upm_role.role_name IS '角色名称';
COMMENT ON COLUMN upm_role.role_desc IS '角色描述';
COMMENT ON COLUMN upm_role.data_scope IS '数据权限类型 1-全部 2-本部门及以下 3-本部门 4-自定义部门';
COMMENT ON COLUMN upm_role.operable IS '是否可操作 1-是 0-否';
COMMENT ON COLUMN upm_role.usable IS '是否可用 1-启用 0-禁用';
COMMENT ON COLUMN upm_role.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_role.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_role.expand IS '扩展字段';
COMMENT ON COLUMN upm_role.version IS '版本号';
COMMENT ON COLUMN upm_role.create_id IS '创建人id';
COMMENT ON COLUMN upm_role.create_time IS '创建时间';
COMMENT ON COLUMN upm_role.update_time IS '修改时间';

CREATE OR REPLACE FUNCTION update_upm_role_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_role_update_time
    BEFORE UPDATE ON upm_role
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_role_update_time();

INSERT INTO upm_role (id, role_code, role_name, role_desc, data_scope, operable, usable, deleted, tenant_id, version, create_time, update_time, expand, create_id)
VALUES (10, 'platformAdmin', '平台管理员', NULL, 1, 0, 1, 0, 1, 1, '2025-06-05 11:01:54.860', '2025-06-20 10:40:01.308', NULL, 1);
INSERT INTO upm_role (id, role_code, role_name, role_desc, data_scope, operable, usable, deleted, tenant_id, version, create_time, update_time, expand, create_id)
VALUES (20, 'tenantAdmin', '租户管理员', NULL, 2, 0, 1, 0, 1, 3, '2025-06-11 11:39:18.116', '2025-06-20 10:53:07.365', NULL, 1);
INSERT INTO upm_role (id, role_code, role_name, role_desc, data_scope, operable, usable, deleted, tenant_id, version, create_time, update_time, expand, create_id)
VALUES (30, 'deptAdmin', '部门管理员', NULL, 2, 0, 1, 0, 1, 5, '2025-06-05 11:03:13.851', '2025-06-20 13:49:48.567', NULL, 1);


-- 权限表
CREATE TABLE upm_permission (
    id BIGINT NOT NULL,
    parent_id BIGINT,
    menu_icon VARCHAR(50) DEFAULT '',
    component VARCHAR(50) DEFAULT '',
    cache SMALLINT DEFAULT 0,
    link SMALLINT DEFAULT 0,
    menu_url VARCHAR(255) DEFAULT '',
    sort_number INT,
    permission_code VARCHAR(50) DEFAULT '',
    permission_name VARCHAR(100) DEFAULT '',
    permission_type SMALLINT,
    operable SMALLINT DEFAULT 1,
    usable SMALLINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1,
    version INT DEFAULT 1,
    device VARCHAR(20) DEFAULT '',
    create_id BIGINT DEFAULT 1,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
CREATE INDEX idx_permission_parent_id ON upm_permission(parent_id);
COMMENT ON TABLE upm_permission IS '权限表';
COMMENT ON COLUMN upm_permission.id IS '主键ID';
COMMENT ON COLUMN upm_permission.parent_id IS '父id';
COMMENT ON COLUMN upm_permission.menu_icon IS 'icon';
COMMENT ON COLUMN upm_permission.component IS '组件';
COMMENT ON COLUMN upm_permission.cache IS '缓存 1-是 0-否';
COMMENT ON COLUMN upm_permission.link IS '外链 1-是 0-否';
COMMENT ON COLUMN upm_permission.menu_url IS '路由';
COMMENT ON COLUMN upm_permission.sort_number IS '排序';
COMMENT ON COLUMN upm_permission.permission_code IS '权限编码';
COMMENT ON COLUMN upm_permission.permission_name IS '权限名称';
COMMENT ON COLUMN upm_permission.permission_type IS '类型 0-目录 1-菜单 2-按钮';
COMMENT ON COLUMN upm_permission.operable IS '是否可操作 1-是 0-否';
COMMENT ON COLUMN upm_permission.usable IS '是否可用 1-启用 0-禁用';
COMMENT ON COLUMN upm_permission.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_permission.version IS '版本号';
COMMENT ON COLUMN upm_permission.device IS '设备端';
COMMENT ON COLUMN upm_permission.create_id IS '创建人id';
COMMENT ON COLUMN upm_permission.create_time IS '创建时间';
COMMENT ON COLUMN upm_permission.update_time IS '修改时间';

CREATE OR REPLACE FUNCTION update_upm_permission_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_permission_update_time
    BEFORE UPDATE ON upm_permission
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_permission_update_time();

INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (10, 0, 'carbon:align-box-bottom-right', 'system', 1, NULL, '系统管理', 0, 0, 1, 0, 1, 1, '2025-06-05 16:15:23.519', '2025-06-10 11:51:45.730', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (110, 10, 'carbon:user-profile', 'user', 40, '', '用户管理', 1, 0, 1, 0, 1, 3, '2025-06-06 14:58:09.033', '2025-06-20 09:36:09.317', 'system/user/index', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (111, 110, NULL, NULL, 1, 'system:user:list', '列表', 2, 0, 1, 0, 1, 5, '2025-06-06 15:08:56.687', '2025-06-20 09:36:09.687', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (112, 110, NULL, NULL, 2, 'system:user:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:09.818', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (113, 110, NULL, NULL, 3, 'system:user:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:09.937', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (114, 110, NULL, NULL, 4, 'system:user:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.048', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (115, 110, NULL, NULL, 5, 'system:user:dataScope', '数据权限', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.165', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (116, 110, NULL, NULL, 6, 'system:user:updatePwd', '重置密码', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.165', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (120, 10, 'ic:sharp-menu', 'menu', 20, '', '菜单管理', 1, 0, 1, 0, 1, 1, '2025-06-09 13:35:10.730', '2025-06-20 09:36:10.268', 'system/menu/index', 1, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (121, 120, NULL, NULL, 1, 'system:menu:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:10.402', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (122, 120, NULL, NULL, 2, 'system:menu:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:43.538', '2025-06-20 09:36:10.535', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (123, 120, NULL, NULL, 3, 'system:menu:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:05:08.738', '2025-06-20 09:36:10.676', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (124, 120, NULL, NULL, 4, 'system:menu:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:10.787', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (130, 10, 'mingcute:department-line', 'dept', 50, '', '部门管理', 1, 0, 1, 0, 1, 1, '2025-06-09 14:44:48.238', '2025-06-20 09:36:11.132', 'system/dept/index', 1, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (131, 130, NULL, NULL, 1, 'system:dept:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.239', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (132, 130, NULL, NULL, 2, 'system:dept:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.299', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (133, 130, NULL, NULL, 3, 'system:dept:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.419', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (134, 130, NULL, NULL, 4, 'system:dept:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.584', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (140, 10, 'carbon:align-vertical-center', 'post', 60, '', '岗位管理', 1, 0, 1, 0, 1, 2, '2025-06-10 10:49:45.307', '2025-06-20 09:36:11.655', 'system/post/index', 1, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (141, 140, NULL, NULL, 1, 'system:position:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.746', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (142, 140, NULL, NULL, 2, 'system:position:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.830', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (143, 140, NULL, NULL, 3, 'system:position:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.908', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (144, 140, NULL, NULL, 4, 'system:position:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.998', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (150, 10, 'ph:users-light', 'tenant', 10, '', '租户管理', 1, 0, 1, 0, 1, 3, '2025-06-10 15:34:04.749', '2025-06-20 09:36:12.088', 'system/tenant/index', 1, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (151, 150, NULL, NULL, 1, 'system:tenant:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.165', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (152, 150, NULL, NULL, 2, 'system:tenant:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.232', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (153, 150, NULL, NULL, 3, 'system:tenant:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.308', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (154, 150, NULL, NULL, 4, 'system:tenant:add', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.377', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (160, 10, 'carbon:scis-control-tower', 'role', 30, '', '角色管理', 1, 0, 1, 0, 1, 3, '2025-06-10 15:45:32.109', '2025-06-20 09:36:12.466', 'system/role/index', 1, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (161, 160, NULL, NULL, 1, 'system:role:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.586', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (162, 160, NULL, NULL, 2, 'system:role:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.650', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (163, 160, NULL, NULL, 3, 'system:role:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.741', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (164, 160, NULL, NULL, 4, 'system:role:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.868', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (165, 160, NULL, NULL, 5, 'system:role:bindUser', '分配用户', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.944', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (166, 160, NULL, NULL, 6, 'system:role:permission', '授权', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.944', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (170, 10, 'material-symbols:logo-dev-outline', 'log', 70, NULL, '日志管理', 1, 0, 1, 0, 1, 2, '2025-06-16 16:29:04.538', '2025-06-20 09:36:13.033', 'system/log/index', 1, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id)
VALUES (171, 170, NULL, NULL, 1, 'system:log:list', '列表', 2, 0, 1, 0, 1, 6, '2025-06-16 16:29:54.313', '2025-06-20 09:36:13.108', '', 0, 0, 'web', 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, component, cache, link, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, device, create_id, create_time, update_time)
VALUES (180, 10, '', '', 0, 1, 'http://localhost:9003/doc.html', 80, '', '接口', 1, 1, 1, 0, 1, 48, 'web', 10, '2025-07-02 15:47:00.771', '2026-01-23 16:05:44.405');


-- 角色权限关联表
CREATE TABLE upm_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    tenant_id BIGINT DEFAULT 1,
    device VARCHAR(20) DEFAULT '',
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);
CREATE INDEX idx_role_permission_role_id ON upm_role_permission(role_id);
COMMENT ON TABLE upm_role_permission IS '角色权限关联表';
COMMENT ON COLUMN upm_role_permission.role_id IS '角色id';
COMMENT ON COLUMN upm_role_permission.permission_id IS '权限id';
COMMENT ON COLUMN upm_role_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_role_permission.device IS '设备端';

INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 10, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 110, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 111, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 112, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 113, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 114, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 115, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 116, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 120, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 121, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 122, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 123, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 124, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 130, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 131, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 132, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 133, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 134, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 140, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 141, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 142, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 143, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 144, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 150, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 151, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 152, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 153, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 154, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 160, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 161, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 162, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 163, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 164, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 165, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 166, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 170, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 171, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 180, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 10, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 110, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 111, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 112, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 113, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 114, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 115, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 116, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 130, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 131, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 132, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 133, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 134, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 140, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 141, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 142, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 143, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 144, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 160, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 161, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 162, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 163, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 164, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 165, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 166, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 170, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 171, 1, 'web');


-- 日志表
CREATE TABLE upm_log (
    id BIGINT NOT NULL,
    module VARCHAR(50),
    action VARCHAR(50),
    success SMALLINT,
    operator VARCHAR(50),
    method VARCHAR(255),
    params TEXT,
    ip VARCHAR(20),
    region VARCHAR(30),
    duration INT,
    deleted SMALLINT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1,
    dept_id BIGINT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    stack_trace TEXT,
    PRIMARY KEY (id)
);
CREATE INDEX idx_create_time ON upm_log(create_time);
COMMENT ON TABLE upm_log IS '日志表';
COMMENT ON COLUMN upm_log.id IS '主键ID';
COMMENT ON COLUMN upm_log.module IS '模块';
COMMENT ON COLUMN upm_log.action IS '动作 增删改';
COMMENT ON COLUMN upm_log.success IS '是否成功 1-是 0-否';
COMMENT ON COLUMN upm_log.operator IS '操作人';
COMMENT ON COLUMN upm_log.method IS '请求方法';
COMMENT ON COLUMN upm_log.params IS '请求参数';
COMMENT ON COLUMN upm_log.ip IS '请求ip';
COMMENT ON COLUMN upm_log.region IS '地区';
COMMENT ON COLUMN upm_log.duration IS '耗时 毫秒';
COMMENT ON COLUMN upm_log.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_log.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_log.dept_id IS '部门ID';
COMMENT ON COLUMN upm_log.create_time IS '创建时间';
COMMENT ON COLUMN upm_log.stack_trace IS '异常堆栈';


-- 角色部门关联表
CREATE TABLE upm_role_dept (
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    tenant_id BIGINT DEFAULT 1,
    CONSTRAINT uk_role_dept UNIQUE (role_id, dept_id)
);
CREATE INDEX idx_role_dept_role_id ON upm_role_dept(role_id);
COMMENT ON TABLE upm_role_dept IS '角色部门关联表';
COMMENT ON COLUMN upm_role_dept.role_id IS '角色ID';
COMMENT ON COLUMN upm_role_dept.dept_id IS '部门ID';
COMMENT ON COLUMN upm_role_dept.tenant_id IS '租户ID';


-- 用户角色关联表
CREATE TABLE upm_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    tenant_id BIGINT DEFAULT 1,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);
CREATE INDEX idx_user_role_user_id ON upm_user_role(user_id);
CREATE INDEX idx_user_role_role_id ON upm_user_role(role_id);
COMMENT ON TABLE upm_user_role IS '用户角色关联表';
COMMENT ON COLUMN upm_user_role.user_id IS '用户ID';
COMMENT ON COLUMN upm_user_role.role_id IS '角色ID';
COMMENT ON COLUMN upm_user_role.tenant_id IS '租户ID';

INSERT INTO upm_user_role (user_id, role_id, tenant_id) VALUES (10, 10, 1);


-- 角色-表列忽略关联表
CREATE TABLE upm_role_column (
    role_id BIGINT NOT NULL,
    table_name VARCHAR(30) DEFAULT '',
    ignore_column VARCHAR(500) DEFAULT '',
    tenant_id BIGINT DEFAULT 1,
    CONSTRAINT uk_role_id_table_name UNIQUE (role_id, table_name)
);
CREATE INDEX idx_role_column_role_id ON upm_role_column(role_id);
COMMENT ON TABLE upm_role_column IS '角色表列关联表';
COMMENT ON COLUMN upm_role_column.role_id IS '角色ID';
COMMENT ON COLUMN upm_role_column.table_name IS '表名';
COMMENT ON COLUMN upm_role_column.ignore_column IS '忽略字段 多个用逗号分隔';
COMMENT ON COLUMN upm_role_column.tenant_id IS '租户ID';


-- 用户第三方关联表
CREATE TABLE upm_user_third (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    platform VARCHAR(30) DEFAULT 'wx',
    open_id VARCHAR(60) DEFAULT '',
    union_id VARCHAR(60) DEFAULT '',
    session_key VARCHAR(100) DEFAULT '',
    expand VARCHAR(1000) DEFAULT '',
    tenant_id BIGINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
COMMENT ON TABLE upm_user_third IS '用户第三方关联表';
COMMENT ON COLUMN upm_user_third.id IS '主键ID';
COMMENT ON COLUMN upm_user_third.user_id IS '用户ID';
COMMENT ON COLUMN upm_user_third.platform IS '平台';
COMMENT ON COLUMN upm_user_third.open_id IS 'openId';
COMMENT ON COLUMN upm_user_third.union_id IS 'unionId';
COMMENT ON COLUMN upm_user_third.session_key IS 'sessionKey';
COMMENT ON COLUMN upm_user_third.expand IS '扩展字段';
COMMENT ON COLUMN upm_user_third.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_user_third.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_user_third.create_time IS '创建时间';
COMMENT ON COLUMN upm_user_third.update_time IS '修改时间';

CREATE OR REPLACE FUNCTION update_upm_user_third_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_user_third_update_time
    BEFORE UPDATE ON upm_user_third
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_user_third_update_time();


-- 租户表 (id 自增)
CREATE TABLE upm_tenant (
    id BIGSERIAL,
    name VARCHAR(30) NOT NULL,
    code VARCHAR(30) NOT NULL,
    description VARCHAR(500) DEFAULT '',
    expand VARCHAR(1000) DEFAULT '',
    operable SMALLINT DEFAULT 1,
    usable SMALLINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    version INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_tenant_code ON upm_tenant(code);
COMMENT ON TABLE upm_tenant IS '租户表';
COMMENT ON COLUMN upm_tenant.id IS '主键ID，自增';
COMMENT ON COLUMN upm_tenant.name IS '租户名称';
COMMENT ON COLUMN upm_tenant.code IS '租户编码';
COMMENT ON COLUMN upm_tenant.description IS '租户描述';
COMMENT ON COLUMN upm_tenant.expand IS '扩展字段';
COMMENT ON COLUMN upm_tenant.operable IS '是否可操作 1-是 0-否';
COMMENT ON COLUMN upm_tenant.usable IS '是否可用 1-启用 0-禁用';
COMMENT ON COLUMN upm_tenant.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_tenant.version IS '版本号';
COMMENT ON COLUMN upm_tenant.create_time IS '创建时间';
COMMENT ON COLUMN upm_tenant.update_time IS '更新时间';

CREATE OR REPLACE FUNCTION update_upm_tenant_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_tenant_update_time
    BEFORE UPDATE ON upm_tenant
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_tenant_update_time();

INSERT INTO upm_tenant (id, name, code, description, expand, operable, usable, deleted, version, create_time, update_time)
VALUES (1, '默认租户', 'yilers.com', '默认租户', '{"logo":"https://files.authing.co/user-contents/photos/b97119c3-5772-4a2d-804b-b9727c4cd124.png","name":"UPM"}', 0, 1, 0, 1, '2025-06-09 15:48:25', '2026-01-23 15:51:35');


-- 设备表 (id 自增)
CREATE TABLE upm_device (
    id BIGSERIAL,
    name VARCHAR(30) NOT NULL,
    code VARCHAR(30) NOT NULL,
    description VARCHAR(500) DEFAULT '',
    expand VARCHAR(1000) DEFAULT '',
    operable SMALLINT DEFAULT 1,
    usable SMALLINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    version INT DEFAULT 1,
    tenant_id BIGINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_device_code ON upm_device(code);
COMMENT ON TABLE upm_device IS '设备表';
COMMENT ON COLUMN upm_device.id IS '主键ID，自增';
COMMENT ON COLUMN upm_device.name IS '名称';
COMMENT ON COLUMN upm_device.code IS '编码';
COMMENT ON COLUMN upm_device.description IS '描述';
COMMENT ON COLUMN upm_device.expand IS '扩展字段';
COMMENT ON COLUMN upm_device.operable IS '是否可操作 1-是 0-否';
COMMENT ON COLUMN upm_device.usable IS '是否可用 1-启用 0-禁用';
COMMENT ON COLUMN upm_device.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_device.version IS '版本号';
COMMENT ON COLUMN upm_device.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_device.create_time IS '创建时间';
COMMENT ON COLUMN upm_device.update_time IS '更新时间';

CREATE OR REPLACE FUNCTION update_upm_device_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_device_update_time
    BEFORE UPDATE ON upm_device
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_device_update_time();

INSERT INTO upm_device (id, name, code, description, expand, operable, usable, deleted, version, tenant_id, create_time, update_time)
VALUES (10, 'web端', 'web', '', '', 0, 1, 0, 1, 1, '2025-06-11 09:13:35', '2025-06-11 09:13:35');
INSERT INTO upm_device (id, name, code, description, expand, operable, usable, deleted, version, tenant_id, create_time, update_time)
VALUES (20, 'app端', 'app', '', '', 0, 1, 0, 1, 1, '2026-01-23 15:05:36', '2026-01-23 15:05:36');


-- 用户仅自己数据权限
CREATE TABLE upm_user_data_scope (
    user_id BIGINT NOT NULL,
    interface_path VARCHAR(100) NOT NULL,
    data_scope SMALLINT DEFAULT 1,
    expand VARCHAR(500) DEFAULT '',
    tenant_id BIGINT DEFAULT 1,
    CONSTRAINT uk_user_interface UNIQUE (user_id, interface_path)
);
CREATE INDEX idx_user_data_scope_user_id ON upm_user_data_scope(user_id);
COMMENT ON TABLE upm_user_data_scope IS '用户-数据权限';
COMMENT ON COLUMN upm_user_data_scope.user_id IS '用户ID';
COMMENT ON COLUMN upm_user_data_scope.interface_path IS '接口路径';
COMMENT ON COLUMN upm_user_data_scope.data_scope IS '数据权限类型 1-全部 2-本部门及以下 3-本部门 4-自定义部门 5-仅自己';
COMMENT ON COLUMN upm_user_data_scope.expand IS '扩展字段';
COMMENT ON COLUMN upm_user_data_scope.tenant_id IS '租户ID';


-- 部门主管配置
CREATE TABLE upm_dept_leader (
    id BIGINT,
    dept_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    leader_type SMALLINT NOT NULL DEFAULT 1,
    remark VARCHAR(50) DEFAULT '',
    start_date DATE NOT NULL,
    end_date DATE DEFAULT NULL,
    tenant_id BIGINT DEFAULT 1,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);
COMMENT ON TABLE upm_dept_leader IS '部门主管配置';
COMMENT ON COLUMN upm_dept_leader.id IS '主键ID';
COMMENT ON COLUMN upm_dept_leader.dept_id IS '部门ID';
COMMENT ON COLUMN upm_dept_leader.user_id IS '主管员工ID';
COMMENT ON COLUMN upm_dept_leader.leader_type IS '主管类型 待定';
COMMENT ON COLUMN upm_dept_leader.remark IS '备注';
COMMENT ON COLUMN upm_dept_leader.start_date IS '任职开始日期';
COMMENT ON COLUMN upm_dept_leader.end_date IS '任职结束日期, NULL表示当前任职';
COMMENT ON COLUMN upm_dept_leader.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_dept_leader.create_time IS '创建时间';
COMMENT ON COLUMN upm_dept_leader.update_time IS '更新时间';

CREATE OR REPLACE FUNCTION update_upm_dept_leader_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_dept_leader_update_time
    BEFORE UPDATE ON upm_dept_leader
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_dept_leader_update_time();