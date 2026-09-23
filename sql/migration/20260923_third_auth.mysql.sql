-- MySQL 5.7 / 8.x 第三方认证配置增量迁移。停服、备份后执行一次，不可重复执行。
-- 前置条件：已经完成应用及SSO迁移；执行前先阅读同目录README.md。
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

-- 为每个租户添加第三方认证配置入口及四个按钮。
-- 迁移期间必须停止写入；新菜单ID使用现有最大ID之后的连续区间。
-- 兼容MySQL 5.7：按租户主键计算序号，不使用窗口函数和用户变量。
CREATE TEMPORARY TABLE migration_third_auth_menu AS
SELECT t.id AS tenant_id, infra.id AS app_id, COALESCE(root.parent_id, 0) AS parent_id,
       base.max_id + ((
           SELECT COUNT(*) FROM upm_tenant preceding_tenant
           WHERE preceding_tenant.deleted = 0 AND preceding_tenant.id <= t.id
       ) - 1) * 5 + 1 AS menu_id
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

DROP TEMPORARY TABLE migration_third_auth_menu;
