-- 租户应用菜单同步。
-- 执行前停止写入并备份数据库；本脚本只为后续新建租户和菜单同步建立来源关系，
-- 不尝试匹配改造前已经复制到其他租户的历史菜单。

ALTER TABLE upm_permission
    ADD COLUMN source_id BIGINT DEFAULT NULL;

COMMENT ON COLUMN upm_permission.source_id IS '租户1模板菜单ID';

CREATE UNIQUE INDEX uk_permission_tenant_source
    ON upm_permission (tenant_id, source_id);

UPDATE upm_permission
SET permission_code = 'system:tenant:delete'
WHERE id = 154 AND tenant_id = 1 AND permission_code = 'system:tenant:add';

INSERT INTO upm_permission
    (id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
     sort_number, device, operable, usable, deleted, version)
VALUES
    (195, 150, 1, 1, '同步应用菜单', 2, 'system:tenant:sync', 5, 'web', 0, 1, 0, 1);

INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device)
VALUES (10, 195, 1, 'web');
