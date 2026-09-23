-- PostgreSQL 第三方认证配置增量迁移。停服、备份后执行一次，不可重复执行。
-- 前置条件：已经完成应用及SSO迁移；执行前先阅读同目录README.md。
BEGIN;

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

-- 为每个租户添加第三方认证配置入口及四个按钮。
-- 迁移期间必须停止写入；新菜单ID使用现有最大ID之后的连续区间。
CREATE TEMPORARY TABLE migration_third_auth_menu AS
SELECT t.id AS tenant_id, infra.id AS app_id, COALESCE(root.parent_id, 0) AS parent_id,
       base.max_id + (ROW_NUMBER() OVER (ORDER BY t.id) - 1) * 5 + 1 AS menu_id
FROM upm_tenant t
CROSS JOIN (SELECT COALESCE(MAX(id), 0) AS max_id FROM upm_permission) base
JOIN upm_application infra ON infra.tenant_id = t.id AND infra.code = 'infra' AND infra.deleted = 0
LEFT JOIN (
    SELECT tenant_id, MIN(parent_id) AS parent_id
    FROM upm_permission
    WHERE component = 'system/application/index' AND device = 'web' AND deleted = 0
    GROUP BY tenant_id
) root ON root.tenant_id = t.id
WHERE t.deleted = 0;

INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, menu_url, component, menu_icon, sort_number, device, operable, usable, deleted, version)
SELECT menu_id, parent_id, app_id, tenant_id, '第三方认证', 1, 'third-auth', 'system/third-auth/index', 'simple-icons:authy', 16, 'web', 0, 1, 0, 1
FROM migration_third_auth_menu;

INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
SELECT m.menu_id + b.offset_id, m.menu_id, m.app_id, m.tenant_id, b.name, 2, b.code, b.offset_id, 'web', 0, 1, 0, 1
FROM migration_third_auth_menu m
CROSS JOIN (
    SELECT 1 AS offset_id, '列表' AS name, 'system:thirdAuth:list' AS code
    UNION ALL SELECT 2, '新增', 'system:thirdAuth:add'
    UNION ALL SELECT 3, '修改', 'system:thirdAuth:edit'
    UNION ALL SELECT 4, '启停', 'system:thirdAuth:usable'
) b;

INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device)
SELECT r.id, p.id, r.tenant_id, 'web'
FROM upm_role r
JOIN migration_third_auth_menu m ON m.tenant_id = r.tenant_id
JOIN upm_permission p ON p.id BETWEEN m.menu_id AND m.menu_id + 4
WHERE r.deleted = 0 AND r.role_code IN ('platformAdmin', 'tenantAdmin');

DROP TABLE migration_third_auth_menu;
COMMIT;
