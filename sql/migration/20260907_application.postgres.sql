-- PostgreSQL 应用改造增量迁移。停服、备份后执行一次，不可重复执行。
-- 执行前先阅读同目录 README.md；此文件不会由应用自动执行。
BEGIN;
CREATE TABLE upm_application (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64) NOT NULL,
    icon VARCHAR(255) DEFAULT '',
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

ALTER TABLE upm_permission ADD COLUMN app_id BIGINT;

-- 应用归属到租户；已有菜单id、父子关系、角色授权保持不变。
INSERT INTO upm_application (id, tenant_id, name, code, icon, description, sort_number, operable, usable, deleted, version)
SELECT id, id, '基础管理平台', 'infra', 'lucide:shield-check', '内置租户、用户、角色、菜单等基础管理功能', 0, 0, 1, 0, 1
FROM upm_tenant;

UPDATE upm_permission SET app_id = tenant_id;

-- 为每个租户添加应用管理入口及四个按钮，保留原有菜单记录。
-- 迁移期间必须停止写入；新菜单id使用现有最大id之后的连续区间。
CREATE TEMPORARY TABLE migration_application_menu AS
SELECT t.id AS tenant_id, COALESCE(root.parent_id, 0) AS parent_id,
       base.max_id + ROW_NUMBER() OVER (ORDER BY t.id) * 5 AS menu_id
FROM upm_tenant t
CROSS JOIN (SELECT COALESCE(MAX(id), 0) AS max_id FROM upm_permission) base
LEFT JOIN (
    SELECT tenant_id, MIN(parent_id) AS parent_id FROM upm_permission
    WHERE component = 'system/menu/index' AND device = 'web' AND deleted = 0
    GROUP BY tenant_id
) root ON root.tenant_id = t.id
WHERE t.deleted = 0;

INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, menu_url, component, menu_icon, sort_number, device, operable, usable, deleted, version)
SELECT menu_id, parent_id, tenant_id, tenant_id, '应用管理', 1, 'application', 'system/application/index', 'lucide:app-window', 15, 'web', 0, 1, 0, 1
FROM migration_application_menu;

INSERT INTO upm_permission (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code, sort_number, device, operable, usable, deleted, version)
SELECT m.menu_id + b.offset_id, m.menu_id, m.tenant_id, m.tenant_id, b.name, 2, b.code, b.offset_id, 'web', 0, 1, 0, 1
FROM migration_application_menu m
CROSS JOIN (
    SELECT 1 AS offset_id, '列表' AS name, 'system:application:list' AS code
    UNION ALL SELECT 2, '新增', 'system:application:add'
    UNION ALL SELECT 3, '修改', 'system:application:edit'
    UNION ALL SELECT 4, '启停', 'system:application:usable'
) b;

INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device)
SELECT r.id, p.id, r.tenant_id, 'web'
FROM upm_role r
JOIN migration_application_menu m ON m.tenant_id = r.tenant_id
JOIN upm_permission p ON p.id BETWEEN m.menu_id AND m.menu_id + 4
WHERE r.deleted = 0 AND r.role_code IN ('platformAdmin', 'tenantAdmin');

DROP TABLE migration_application_menu;

ALTER TABLE upm_permission ALTER COLUMN app_id SET NOT NULL;
CREATE INDEX idx_permission_app_device ON upm_permission (tenant_id, app_id, device, parent_id);
COMMIT;
