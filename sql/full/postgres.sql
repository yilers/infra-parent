-- PostgreSQL 全量初始化：仅在空数据库执行一次。
-- 已包含截至2026-09-07的应用改造，不要再执行同版本增量迁移。
-- 执行前阅读 sql/README.md；已有数据库请使用 sql/migration/。
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
VALUES (10, 0, 'yilers', 'yilers', NULL, 1, 1, 1, 1, 0, 1, 1, '2026-01-08 15:42:17.808', '2026-01-08 15:42:17.808');


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
VALUES (10, 'dev', '开发', NULL, 1, 1, 1, 0, 1, 1, '2025-06-05 10:50:30.000', '2025-06-05 10:50:32.000');
INSERT INTO upm_position (id, position_code, position_name, position_desc, sort_number, operable, usable, deleted, tenant_id, version, create_time, update_time)
VALUES (20, 'user', '员工', NULL, 2, 1, 1, 0, 1, 1, '2025-06-05 11:07:01.740', '2025-06-05 11:07:01.740');


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
VALUES (10, 'admin', 'platform@yilers.com', '超级管理员', '$2a$10$1FylLzVNqSTschwjVyADUOcDplWdLJk2cF1N/5pd8yrep9O2RTVQa', 10, 10, '超级管理员', 1, NULL, NULL, NULL, NULL, 1, 1, 0, 1, 1, 1, '{"initPwd":true}', '2025-06-05 10:42:24.565', '2026-01-23 15:52:13.989');
INSERT INTO upm_user (id, user_type, account, nickname, password, dept_id, position_id, name, gender, photo, id_card, email, phone, operable, usable, deleted, tenant_id, version, create_id, expand, create_time, update_time)
VALUES (20, 'admin', 'admin@yilers.com', '租户管理员', '$2a$10$1FylLzVNqSTschwjVyADUOcDplWdLJk2cF1N/5pd8yrep9O2RTVQa', 10, 10, '租户管理员', 1, '', '', '', '', 1, 1, 0, 1, 23, 10, '{"initPwd":true}', '2025-06-23 17:59:34.427', '2026-01-23 15:54:59.776');


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
CREATE TABLE upm_application (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64) NOT NULL,
    icon VARCHAR(255) DEFAULT '',
    logo VARCHAR(255) DEFAULT '',
    home_url VARCHAR(500) DEFAULT '',
    sso_enabled SMALLINT NOT NULL DEFAULT 0,
    sso_secret VARCHAR(512) DEFAULT NULL,
    redirect_uris TEXT,
    sso_push_url VARCHAR(500) DEFAULT '',
    secret_update_time TIMESTAMP(3) DEFAULT NULL,
    description VARCHAR(500) DEFAULT '',
    sort_number INT NOT NULL DEFAULT 0,
    operable SMALLINT NOT NULL DEFAULT 1,
    usable SMALLINT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    create_id BIGINT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_application_tenant_code UNIQUE (tenant_id, code, deleted)
);

CREATE TABLE upm_permission (
    app_id BIGINT NOT NULL,
    id BIGINT NOT NULL,
    source_id BIGINT DEFAULT NULL,
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
CREATE UNIQUE INDEX uk_permission_tenant_source ON upm_permission(tenant_id, source_id);
COMMENT ON TABLE upm_permission IS '权限表';
COMMENT ON COLUMN upm_permission.id IS '主键ID';
COMMENT ON COLUMN upm_permission.source_id IS '租户1模板菜单ID';
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

INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (10, 0, 'carbon:align-box-bottom-right', 'system', 1, NULL, '系统管理', 0, 0, 1, 0, 1, 1, '2025-06-05 16:15:23.519', '2025-06-10 11:51:45.730', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (110, 10, 'carbon:user-profile', 'user', 40, '', '用户管理', 1, 0, 1, 0, 1, 3, '2025-06-06 14:58:09.033', '2025-06-20 09:36:09.317', 'system/user/index', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (111, 110, NULL, NULL, 1, 'system:user:list', '列表', 2, 0, 1, 0, 1, 5, '2025-06-06 15:08:56.687', '2025-06-20 09:36:09.687', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (112, 110, NULL, NULL, 2, 'system:user:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:09.818', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (113, 110, NULL, NULL, 3, 'system:user:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:09.937', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (114, 110, NULL, NULL, 4, 'system:user:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.048', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (115, 110, NULL, NULL, 5, 'system:user:dataScope', '数据权限', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.165', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (116, 110, NULL, NULL, 6, 'system:user:updatePwd', '重置密码', 2, 0, 1, 0, 1, 1, '2025-06-12 14:37:35.951', '2025-06-20 09:36:10.165', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (117, 110, NULL, NULL, 7, 'system:user:unlock', '解除登录锁定', 2, 1, 1, 0, 1, 1, '2026-09-29 00:00:00.000', '2026-09-29 00:00:00.000', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (118, 110, NULL, NULL, 8, 'system:user:batch:add', '批量新增', 2, 1, 1, 0, 1, 1, '2026-09-30 00:00:00.000', '2026-09-30 00:00:00.000', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (120, 10, 'ic:sharp-menu', 'menu', 20, '', '菜单管理', 1, 0, 1, 0, 1, 1, '2025-06-09 13:35:10.730', '2025-06-20 09:36:10.268', 'system/menu/index', 1, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (121, 120, NULL, NULL, 1, 'system:menu:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:10.402', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (122, 120, NULL, NULL, 2, 'system:menu:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:43.538', '2025-06-20 09:36:10.535', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (123, 120, NULL, NULL, 3, 'system:menu:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:05:08.738', '2025-06-20 09:36:10.676', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (124, 120, NULL, NULL, 4, 'system:menu:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:10.787', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (130, 10, 'mingcute:department-line', 'dept', 50, '', '部门管理', 1, 0, 1, 0, 1, 1, '2025-06-09 14:44:48.238', '2025-06-20 09:36:11.132', 'system/dept/index', 1, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (131, 130, NULL, NULL, 1, 'system:dept:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.239', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (132, 130, NULL, NULL, 2, 'system:dept:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.299', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (133, 130, NULL, NULL, 3, 'system:dept:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.419', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (134, 130, NULL, NULL, 4, 'system:dept:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.584', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (196, 130, 5, 'system:dept:leader:edit', '设置主管', 2, 1, 1, 0, 1, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (140, 10, 'carbon:align-vertical-center', 'post', 60, '', '岗位管理', 1, 0, 1, 0, 1, 2, '2025-06-10 10:49:45.307', '2025-06-20 09:36:11.655', 'system/post/index', 1, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (141, 140, NULL, NULL, 1, 'system:position:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.746', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (142, 140, NULL, NULL, 2, 'system:position:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.830', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (143, 140, NULL, NULL, 3, 'system:position:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.908', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (144, 140, NULL, NULL, 4, 'system:position:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:11.998', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (150, 10, 'carbon:workspace', 'tenant', 10, '', '租户管理', 1, 0, 1, 0, 1, 3, '2025-06-10 15:34:04.749', '2025-06-20 09:36:12.088', 'system/tenant/index', 1, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (151, 150, NULL, NULL, 1, 'system:tenant:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.165', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (152, 150, NULL, NULL, 2, 'system:tenant:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.232', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (153, 150, NULL, NULL, 3, 'system:tenant:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.308', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (154, 150, NULL, NULL, 4, 'system:tenant:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.377', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, component, cache, link, device, create_id, app_id)
VALUES (195, 150, NULL, 5, 'system:tenant:sync', '同步应用菜单', 2, 1, 1, 0, 1, 1, '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (160, 10, 'carbon:scis-control-tower', 'role', 30, '', '角色管理', 1, 0, 1, 0, 1, 3, '2025-06-10 15:45:32.109', '2025-06-20 09:36:12.466', 'system/role/index', 1, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (161, 160, NULL, NULL, 1, 'system:role:list', '列表', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.586', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (162, 160, NULL, NULL, 2, 'system:role:add', '新增', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.650', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (163, 160, NULL, NULL, 3, 'system:role:edit', '修改', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.741', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (164, 160, NULL, NULL, 4, 'system:role:delete', '删除', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.868', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (165, 160, NULL, NULL, 5, 'system:role:bindUser', '分配用户', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.944', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (166, 160, NULL, NULL, 6, 'system:role:permission', '授权', 2, 0, 1, 0, 1, 1, '2025-06-10 11:03:04.015', '2025-06-20 09:36:12.944', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (170, 10, 'material-symbols:logo-dev-outline', 'log', 70, NULL, '日志管理', 1, 0, 1, 0, 1, 2, '2025-06-16 16:29:04.538', '2025-06-20 09:36:13.033', 'system/log/index', 1, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, create_time, update_time, component, cache, link, device, create_id, app_id)
VALUES (171, 170, NULL, NULL, 1, 'system:log:list', '列表', 2, 0, 1, 0, 1, 6, '2025-06-16 16:29:54.313', '2025-06-20 09:36:13.108', '', 0, 0, 'web', 1, 1);
INSERT INTO upm_permission (id, parent_id, menu_icon, component, cache, link, menu_url, sort_number, permission_code, permission_name, permission_type, operable, usable, deleted, tenant_id, version, device, create_id, create_time, update_time, app_id)
VALUES (180, 10, 'carbon:doc', '', 0, 1, 'http://localhost:9003/doc.html', 80, '', '接口文档', 1, 1, 1, 0, 1, 48, 'web', 10, '2025-07-02 15:47:00.771', '2026-01-23 16:05:44.405', 1);


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
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 117, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 118, 1, 'web');
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
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 196, 1, 'web');
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
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (10, 195, 1, 'web');
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
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 117, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 118, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 130, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 131, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 132, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 133, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 134, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device) VALUES (20, 196, 1, 'web');
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
    open_id VARCHAR(128) DEFAULT NULL,
    union_id VARCHAR(128) DEFAULT NULL,
    session_key VARCHAR(100) DEFAULT '',
    expand VARCHAR(1000) DEFAULT '',
    tenant_id BIGINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_third_user_platform UNIQUE (tenant_id, user_id, platform),
    CONSTRAINT uk_user_third_open_platform UNIQUE (tenant_id, platform, open_id)
);
CREATE UNIQUE INDEX uk_user_third_union_platform
    ON upm_user_third (tenant_id, platform, union_id)
    WHERE union_id IS NOT NULL AND union_id <> '';
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


-- 第三方认证平台配置表
CREATE TABLE upm_third_auth_config (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    platform VARCHAR(30) NOT NULL,
    client_id VARCHAR(255) DEFAULT '',
    client_secret VARCHAR(512) DEFAULT '',
    redirect_uri VARCHAR(500) DEFAULT '',
    scopes VARCHAR(1000) DEFAULT '',
    description VARCHAR(500) DEFAULT '',
    operable SMALLINT NOT NULL DEFAULT 1,
    usable SMALLINT NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    create_id BIGINT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_third_auth_tenant_platform UNIQUE (tenant_id, platform, deleted)
);
COMMENT ON TABLE upm_third_auth_config IS '第三方认证平台配置表';
COMMENT ON COLUMN upm_third_auth_config.id IS '主键ID';
COMMENT ON COLUMN upm_third_auth_config.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_third_auth_config.platform IS '第三方认证平台编码';
COMMENT ON COLUMN upm_third_auth_config.client_id IS '第三方平台Client ID或AppKey';
COMMENT ON COLUMN upm_third_auth_config.client_secret IS '第三方平台Client Secret或AppSecret';
COMMENT ON COLUMN upm_third_auth_config.redirect_uri IS '第三方平台授权回调地址';
COMMENT ON COLUMN upm_third_auth_config.scopes IS '授权范围，多个以英文逗号分隔';
COMMENT ON COLUMN upm_third_auth_config.description IS '配置说明';
COMMENT ON COLUMN upm_third_auth_config.operable IS '是否可操作 1-是 0-否';
COMMENT ON COLUMN upm_third_auth_config.usable IS '是否启用 1-启用 0-停用';
COMMENT ON COLUMN upm_third_auth_config.deleted IS '是否删除 1-是 0-否';
COMMENT ON COLUMN upm_third_auth_config.version IS '乐观锁版本号';
COMMENT ON COLUMN upm_third_auth_config.create_id IS '创建人ID';
COMMENT ON COLUMN upm_third_auth_config.create_time IS '创建时间';
COMMENT ON COLUMN upm_third_auth_config.update_time IS '更新时间';

CREATE OR REPLACE FUNCTION update_upm_third_auth_config_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_third_auth_config_update_time
    BEFORE UPDATE ON upm_third_auth_config
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_third_auth_config_update_time();


-- 租户表 (id 自增)
CREATE TABLE upm_tenant (
    id BIGINT NOT NULL,
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


-- 租户账号密码登录策略表（物理删除；无记录表示不限制）
CREATE TABLE upm_tenant_login_policy (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    max_failures INT NOT NULL DEFAULT 3,
    failure_window_minutes INT NOT NULL DEFAULT 10,
    lock_duration_minutes INT NOT NULL DEFAULT 10,
    version INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_tenant_login_policy_tenant UNIQUE (tenant_id)
);
COMMENT ON TABLE upm_tenant_login_policy IS '租户账号密码登录策略表';
COMMENT ON COLUMN upm_tenant_login_policy.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_tenant_login_policy.max_failures IS '统计周期内最大连续失败次数';
COMMENT ON COLUMN upm_tenant_login_policy.failure_window_minutes IS '失败统计周期，单位分钟';
COMMENT ON COLUMN upm_tenant_login_policy.lock_duration_minutes IS '账号锁定时长，单位分钟';
COMMENT ON COLUMN upm_tenant_login_policy.version IS '乐观锁版本号';

CREATE OR REPLACE FUNCTION update_upm_tenant_login_policy_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_tenant_login_policy_update_time
    BEFORE UPDATE ON upm_tenant_login_policy
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_tenant_login_policy_update_time();

INSERT INTO upm_tenant_login_policy
    (id, tenant_id, max_failures, failure_window_minutes, lock_duration_minutes, version)
VALUES (1, 1, 3, 10, 10, 1);

-- 设备表 (id 自增)
CREATE TABLE upm_device (
    id BIGINT NOT NULL,
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
COMMENT ON COLUMN upm_dept_leader.leader_type IS '主管类型 1-主管 2-副主管';
COMMENT ON COLUMN upm_dept_leader.remark IS '备注';
COMMENT ON COLUMN upm_dept_leader.start_date IS '任职开始日期';
COMMENT ON COLUMN upm_dept_leader.end_date IS '任职结束日期（不包含当天）, NULL表示仍在任';
COMMENT ON COLUMN upm_dept_leader.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_dept_leader.create_time IS '创建时间';
COMMENT ON COLUMN upm_dept_leader.update_time IS '更新时间';
CREATE INDEX idx_dept_leader_dept ON upm_dept_leader (tenant_id, dept_id, end_date);
CREATE INDEX idx_dept_leader_user ON upm_dept_leader (tenant_id, user_id, end_date);

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

-- Warm-Flow v1.8.9 原生流程表；来源：Dromara/warm-flow v1.8.9 官方 PostgreSQL 全量脚本。
-- 仅创建引擎七张 flow_ 表；项目自有扩展表使用 flow_ext_ 前缀。
CREATE TABLE flow_definition
(
    id              int8         NOT NULL,
    flow_code       varchar(40)  NOT NULL,
    flow_name       varchar(100) NOT NULL,
    model_value     varchar(40)  NOT NULL DEFAULT 'CLASSICS',
    category        varchar(100) NULL,
    "version"       varchar(20)  NOT NULL,
    is_publish      int2         NOT NULL DEFAULT 0,
    form_custom     bpchar(1)    NULL     DEFAULT 'N':: character varying,
    form_path       varchar(100) NULL,
    activity_status int2         NOT NULL DEFAULT 1,
    listener_type   varchar(100) NULL,
    listener_path   varchar(400) NULL,
    ext             varchar(500) NULL,
    create_time     timestamp    NULL,
    create_by       varchar(64)  NULL     DEFAULT '':: character varying,
    update_time     timestamp    NULL,
    update_by       varchar(64)  NULL     DEFAULT '':: character varying,
    del_flag        bpchar(1)    NULL     DEFAULT '0':: character varying,
    tenant_id       varchar(40)  NULL,
    CONSTRAINT flow_definition_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_definition IS '流程定义表';

COMMENT ON COLUMN flow_definition.id IS '主键id';
COMMENT ON COLUMN flow_definition.flow_code IS '流程编码';
COMMENT ON COLUMN flow_definition.flow_name IS '流程名称';
COMMENT ON COLUMN flow_definition.model_value IS '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）';
COMMENT ON COLUMN flow_definition.category IS '流程类别';
COMMENT ON COLUMN flow_definition."version" IS '流程版本';
COMMENT ON COLUMN flow_definition.is_publish IS '是否发布（0未发布 1已发布 9失效）';
COMMENT ON COLUMN flow_definition.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_definition.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_definition.activity_status IS '流程激活状态（0挂起 1激活）';
COMMENT ON COLUMN flow_definition.listener_type IS '监听器类型';
COMMENT ON COLUMN flow_definition.listener_path IS '监听器路径';
COMMENT ON COLUMN flow_definition.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_definition.create_time IS '创建时间';
COMMENT ON COLUMN flow_definition.create_by IS '创建人';
COMMENT ON COLUMN flow_definition.update_time IS '更新时间';
COMMENT ON COLUMN flow_definition.update_by IS '更新人';
COMMENT ON COLUMN flow_definition.del_flag IS '删除标志';
COMMENT ON COLUMN flow_definition.tenant_id IS '租户id';

CREATE TABLE flow_node
(
    id              int8          NOT NULL,
    node_type       int2          NOT NULL,
    definition_id   int8          NOT NULL,
    node_code       varchar(100)  NOT NULL,
    node_name       varchar(100)  NULL,
    permission_flag varchar(200)  NULL,
    node_ratio      varchar(200) NULL,
    coordinate      varchar(100)  NULL,
    any_node_skip   varchar(100)  NULL,
    listener_type   varchar(100)  NULL,
    listener_path   varchar(400)  NULL,
    form_custom     bpchar(1)     NULL DEFAULT 'N':: character varying,
    form_path       varchar(100)  NULL,
    "version"       varchar(20)   NOT NULL,
    create_time     timestamp     NULL,
    create_by       varchar(64)   NULL     DEFAULT '':: character varying,
    update_time     timestamp     NULL,
    update_by       varchar(64)   NULL     DEFAULT '':: character varying,
    ext             text          NULL,
    del_flag        bpchar(1)     NULL DEFAULT '0':: character varying,
    tenant_id       varchar(40)   NULL,
    CONSTRAINT flow_node_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_node IS '流程节点表';

COMMENT ON COLUMN flow_node.id IS '主键id';
COMMENT ON COLUMN flow_node.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_node.definition_id IS '流程定义id';
COMMENT ON COLUMN flow_node.node_code IS '流程节点编码';
COMMENT ON COLUMN flow_node.node_name IS '流程节点名称';
COMMENT ON COLUMN flow_node.permission_flag IS '权限标识（权限类型:权限标识，可以多个，用@@隔开)';
COMMENT ON COLUMN flow_node.node_ratio IS '流程签署比例值';
COMMENT ON COLUMN flow_node.coordinate IS '坐标';
COMMENT ON COLUMN flow_node.any_node_skip IS '任意结点跳转';
COMMENT ON COLUMN flow_node.listener_type IS '监听器类型';
COMMENT ON COLUMN flow_node.listener_path IS '监听器路径';
COMMENT ON COLUMN flow_node.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_node.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_node."version" IS '版本';
COMMENT ON COLUMN flow_node.create_time IS '创建时间';
COMMENT ON COLUMN flow_node.create_by IS '创建人';
COMMENT ON COLUMN flow_node.update_time IS '更新时间';
COMMENT ON COLUMN flow_node.update_by IS '更新人';
COMMENT ON COLUMN flow_node.ext IS '节点扩展属性';
COMMENT ON COLUMN flow_node.del_flag IS '删除标志';
COMMENT ON COLUMN flow_node.tenant_id IS '租户id';


CREATE TABLE flow_skip
(
    id             int8         NOT NULL,
    definition_id  int8         NOT NULL,
    now_node_code  varchar(100) NOT NULL,
    now_node_type  int2         NULL,
    next_node_code varchar(100) NOT NULL,
    next_node_type int2         NULL,
    skip_name      varchar(100) NULL,
    skip_type      varchar(40)  NULL,
    skip_condition varchar(200) NULL,
    coordinate     varchar(100) NULL,
    create_time    timestamp    NULL,
    create_by      varchar(64)  NULL     DEFAULT '':: character varying,
    update_time    timestamp    NULL,
    update_by      varchar(64)  NULL     DEFAULT '':: character varying,
    del_flag       bpchar(1)    NULL DEFAULT '0':: character varying,
    tenant_id      varchar(40)  NULL,
    CONSTRAINT flow_skip_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_skip IS '节点跳转关联表';

COMMENT ON COLUMN flow_skip.id IS '主键id';
COMMENT ON COLUMN flow_skip.definition_id IS '流程定义id';
COMMENT ON COLUMN flow_skip.now_node_code IS '当前流程节点的编码';
COMMENT ON COLUMN flow_skip.now_node_type IS '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_skip.next_node_code IS '下一个流程节点的编码';
COMMENT ON COLUMN flow_skip.next_node_type IS '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_skip.skip_name IS '跳转名称';
COMMENT ON COLUMN flow_skip.skip_type IS '跳转类型（PASS审批通过 REJECT退回）';
COMMENT ON COLUMN flow_skip.skip_condition IS '跳转条件';
COMMENT ON COLUMN flow_skip.coordinate IS '坐标';
COMMENT ON COLUMN flow_skip.create_time IS '创建时间';
COMMENT ON COLUMN flow_skip.create_by IS '创建人';
COMMENT ON COLUMN flow_skip.update_time IS '更新时间';
COMMENT ON COLUMN flow_skip.update_by IS '更新人';
COMMENT ON COLUMN flow_skip.del_flag IS '删除标志';
COMMENT ON COLUMN flow_skip.tenant_id IS '租户id';

CREATE TABLE flow_instance
(
    id              int8         NOT NULL,
    definition_id   int8         NOT NULL,
    business_id     varchar(40)  NOT NULL,
    node_type       int2         NOT NULL,
    node_code       varchar(40)  NOT NULL,
    node_name       varchar(100) NULL,
    variable        text         NULL,
    flow_status     varchar(20)  NOT NULL,
    activity_status int2         NOT NULL DEFAULT 1,
    def_json        text         NULL,
    create_time     timestamp    NULL,
    create_by       varchar(64)  NULL     DEFAULT '':: character varying,
    update_time     timestamp    NULL,
    update_by       varchar(64)  NULL     DEFAULT '':: character varying,
    ext             varchar(500) NULL,
    del_flag        bpchar(1)    NULL     DEFAULT '0':: character varying,
    tenant_id       varchar(40)  NULL,
    CONSTRAINT flow_instance_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_instance IS '流程实例表';

COMMENT ON COLUMN flow_instance.id IS '主键id';
COMMENT ON COLUMN flow_instance.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_instance.business_id IS '业务id';
COMMENT ON COLUMN flow_instance.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_instance.node_code IS '流程节点编码';
COMMENT ON COLUMN flow_instance.node_name IS '流程节点名称';
COMMENT ON COLUMN flow_instance.variable IS '任务变量';
COMMENT ON COLUMN flow_instance.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';
COMMENT ON COLUMN flow_instance.activity_status IS '流程激活状态（0挂起 1激活）';
COMMENT ON COLUMN flow_instance.def_json IS '流程定义json';
COMMENT ON COLUMN flow_instance.create_time IS '创建时间';
COMMENT ON COLUMN flow_instance.create_by IS '创建人';
COMMENT ON COLUMN flow_instance.update_time IS '更新时间';
COMMENT ON COLUMN flow_instance.update_by IS '更新人';
COMMENT ON COLUMN flow_instance.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_instance.del_flag IS '删除标志';
COMMENT ON COLUMN flow_instance.tenant_id IS '租户id';

CREATE TABLE flow_task
(
    id            int8         NOT NULL,
    definition_id int8         NOT NULL,
    instance_id   int8         NOT NULL,
    node_code     varchar(100) NOT NULL,
    node_name     varchar(100) NULL,
    node_type     int2         NOT NULL,
    flow_status      varchar(20)  NOT NULL,
    form_custom   bpchar(1)    NULL DEFAULT 'N':: character varying,
    form_path     varchar(100) NULL,
    create_time   timestamp    NULL,
    create_by     varchar(64)  NULL     DEFAULT '':: character varying,
    update_time   timestamp    NULL,
    update_by     varchar(64)  NULL     DEFAULT '':: character varying,
    del_flag      bpchar(1)    NULL DEFAULT '0':: character varying,
    tenant_id     varchar(40)  NULL,
    CONSTRAINT flow_task_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_task IS '待办任务表';

COMMENT ON COLUMN flow_task.id IS '主键id';
COMMENT ON COLUMN flow_task.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_task.instance_id IS '对应flow_instance表的id';
COMMENT ON COLUMN flow_task.node_code IS '节点编码';
COMMENT ON COLUMN flow_task.node_name IS '节点名称';
COMMENT ON COLUMN flow_task.node_type IS '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_task.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';
COMMENT ON COLUMN flow_task.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_task.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_task.create_time IS '创建时间';
COMMENT ON COLUMN flow_task.create_by IS '创建人';
COMMENT ON COLUMN flow_task.update_time IS '更新时间';
COMMENT ON COLUMN flow_task.update_by IS '更新人';
COMMENT ON COLUMN flow_task.del_flag IS '删除标志';
COMMENT ON COLUMN flow_task.tenant_id IS '租户id';

CREATE TABLE flow_his_task
(
    id               int8         NOT NULL,
    definition_id    int8         NOT NULL,
    instance_id      int8         NOT NULL,
    task_id          int8         NOT NULL,
    node_code        varchar(100) NULL,
    node_name        varchar(100) NULL,
    node_type        int2         NULL,
    target_node_code varchar(200) NULL,
    target_node_name varchar(200) NULL,
    approver         varchar(40)  NULL,
    cooperate_type   int2         NOT NULL DEFAULT 0,
    collaborator     varchar(500)  NULL,
    skip_type        varchar(10)  NULL,
    flow_status      varchar(20)  NOT NULL,
    form_custom      bpchar(1)    NULL     DEFAULT 'N':: character varying,
    form_path        varchar(100) NULL,
    ext              text         NULL,
    message          varchar(500) NULL,
    variable         text         NULL,
    create_time      timestamp    NULL,
    update_time      timestamp    NULL,
    del_flag         bpchar(1)    NULL     DEFAULT '0':: character varying,
    tenant_id        varchar(40)  NULL,
    CONSTRAINT flow_his_task_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE flow_his_task IS '历史任务记录表';

COMMENT ON COLUMN flow_his_task.id IS '主键id';
COMMENT ON COLUMN flow_his_task.definition_id IS '对应flow_definition表的id';
COMMENT ON COLUMN flow_his_task.instance_id IS '对应flow_instance表的id';
COMMENT ON COLUMN flow_his_task.task_id IS '对应flow_task表的id';
COMMENT ON COLUMN flow_his_task.node_code IS '开始节点编码';
COMMENT ON COLUMN flow_his_task.node_name IS '开始节点名称';
COMMENT ON COLUMN flow_his_task.node_type IS '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）';
COMMENT ON COLUMN flow_his_task.target_node_code IS '目标节点编码';
COMMENT ON COLUMN flow_his_task.target_node_name IS '结束节点名称';
COMMENT ON COLUMN flow_his_task.approver IS '审批者';
COMMENT ON COLUMN flow_his_task.cooperate_type IS '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)';
COMMENT ON COLUMN flow_his_task.collaborator IS '协作人';
COMMENT ON COLUMN flow_his_task.skip_type IS '流转类型（PASS通过 REJECT退回 NONE无动作）';
COMMENT ON COLUMN flow_his_task.flow_status IS '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）';
COMMENT ON COLUMN flow_his_task.form_custom IS '审批表单是否自定义（Y是 N否）';
COMMENT ON COLUMN flow_his_task.form_path IS '审批表单路径';
COMMENT ON COLUMN flow_his_task.message IS '审批意见';
COMMENT ON COLUMN flow_his_task.variable IS '任务变量';
COMMENT ON COLUMN flow_his_task.ext IS '扩展字段，预留给业务系统使用';
COMMENT ON COLUMN flow_his_task.create_time IS '任务开始时间';
COMMENT ON COLUMN flow_his_task.update_time IS '审批完成时间';
COMMENT ON COLUMN flow_his_task.del_flag IS '删除标志';
COMMENT ON COLUMN flow_his_task.tenant_id IS '租户id';

CREATE TABLE flow_user
(
    id           int8        NOT NULL,
    "type"       bpchar(1)   NOT NULL,
    processed_by varchar(80) NULL,
    associated   int8        NOT NULL,
    create_time  timestamp    NULL,
    create_by    varchar(64)  NULL     DEFAULT '':: character varying,
    update_time  timestamp    NULL,
    update_by    varchar(64)  NULL     DEFAULT '':: character varying,
    del_flag     bpchar(1)   NULL DEFAULT '0':: character varying,
    tenant_id    varchar(40) NULL,
    CONSTRAINT flow_user_pk PRIMARY KEY (id)
);
CREATE INDEX user_processed_type ON flow_user USING btree (processed_by, type);
CREATE INDEX user_associated_idx ON FLOW_USER USING btree (associated);
COMMENT ON TABLE flow_user IS '流程用户表';

COMMENT ON COLUMN flow_user.id IS '主键id';
COMMENT ON COLUMN flow_user."type" IS '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）';
COMMENT ON COLUMN flow_user.processed_by IS '权限人';
COMMENT ON COLUMN flow_user.associated IS '任务表id';
COMMENT ON COLUMN flow_user.create_time IS '创建时间';
COMMENT ON COLUMN flow_user.create_by IS '创建人';
COMMENT ON COLUMN flow_user.update_time IS '更新时间';
COMMENT ON COLUMN flow_user.update_by IS '更新人';
COMMENT ON COLUMN flow_user.del_flag IS '删除标志';
COMMENT ON COLUMN flow_user.tenant_id IS '租户id';

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

INSERT INTO upm_role_permission(role_id, permission_id, tenant_id, device)
SELECT r.id, p.id, 1, 'web'
FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_flow_task_ids m
WHERE r.tenant_id = 1 AND r.id IN (10, 20) AND p.id BETWEEN m.base_id + 1 AND m.base_id + 2;

DROP TABLE migration_flow_task_ids;

-- 请假申请：仅执行一次，前置已执行20261019_flow_inbox和20261019_flow_task_actions。
-- 必须停写、备份；确认尚无flow_ext_leave表、flow/leave/index菜单和flow:leave:*权限，
-- 且模板租户1的infra应用下恰有一个OA审批目录。其他租户随后同步菜单、授权并重新登录。
-- 不创建、复制或修改任何流程定义；提交时使用当前租户已发布的leave流程。
-- 请假示例业务表；状态实时关联引擎实例，不重复存储。
CREATE TABLE flow_ext_leave (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(40) NOT NULL,
    applicant_id VARCHAR(64) NOT NULL,
    applicant_name VARCHAR(100) NOT NULL,
    leave_type VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration_hours NUMERIC(8,1) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    flow_code VARCHAR(40) NOT NULL,
    instance_id BIGINT,
    version INTEGER NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flow_leave_instance UNIQUE (tenant_id, instance_id)
);
CREATE INDEX idx_flow_leave_mine ON flow_ext_leave(tenant_id, applicant_id, create_time, id);
COMMENT ON TABLE flow_ext_leave IS '请假申请业务表';
COMMENT ON COLUMN flow_ext_leave.id IS '请假申请ID';
COMMENT ON COLUMN flow_ext_leave.tenant_id IS '租户ID，与Flow引擎保持字符串类型';
COMMENT ON COLUMN flow_ext_leave.applicant_id IS '申请人用户ID';
COMMENT ON COLUMN flow_ext_leave.applicant_name IS '申请人姓名快照';
COMMENT ON COLUMN flow_ext_leave.leave_type IS 'PERSONAL事假 SICK病假 ANNUAL年假 COMP_TIME调休 OTHER其他';
COMMENT ON COLUMN flow_ext_leave.start_time IS '请假开始时间';
COMMENT ON COLUMN flow_ext_leave.end_time IS '请假结束时间';
COMMENT ON COLUMN flow_ext_leave.duration_hours IS '请假小时数，0.5的整数倍，不自动计算排班';
COMMENT ON COLUMN flow_ext_leave.reason IS '请假原因，不放入流程变量';
COMMENT ON COLUMN flow_ext_leave.flow_code IS '绑定流程编码，默认由服务端配置为leave';
COMMENT ON COLUMN flow_ext_leave.instance_id IS '流程实例ID，空值表示草稿';
COMMENT ON COLUMN flow_ext_leave.version IS '乐观锁版本';
COMMENT ON COLUMN flow_ext_leave.create_time IS '创建时间';
COMMENT ON COLUMN flow_ext_leave.update_time IS '最后编辑或提交时间';

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

-- 自动抄送：新库全量初始化。后续升级的已有库执行20261020_flow_cc.postgres.sql。
CREATE TABLE flow_cc (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(40) NOT NULL,
    instance_id BIGINT NOT NULL,
    source_task_id BIGINT NOT NULL,
    node_code VARCHAR(100) NOT NULL,
    node_name VARCHAR(200) NOT NULL,
    recipient_id VARCHAR(40) NOT NULL,
    recipient_name VARCHAR(128) NOT NULL,
    create_time TIMESTAMP(3) NOT NULL,
    read_time TIMESTAMP(3),
    CONSTRAINT uk_flow_cc_task_recipient UNIQUE (tenant_id,source_task_id,recipient_id)
);
COMMENT ON TABLE flow_cc IS '流程节点自动抄送接收记录';
CREATE INDEX idx_flow_cc_inbox ON flow_cc(tenant_id,recipient_id,create_time,id);
CREATE INDEX idx_flow_cc_instance ON flow_cc(tenant_id,instance_id);
CREATE TEMPORARY TABLE migration_flow_cc_ids AS
SELECT p.id AS parent_id,p.app_id,(SELECT COALESCE(MAX(id),0) FROM upm_permission) AS base_id
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
