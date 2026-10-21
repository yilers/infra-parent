-- 待办转办与委派按钮。仅执行一次，前置已执行20261019_flow_task_actions。
-- 停写、备份；确认模板租户1的flow/todo/index仅一条，且不存在下列两个权限编码。
-- ID从现有最大菜单ID后连续分配2个。
CREATE TEMPORARY TABLE migration_flow_transfer_ids AS
SELECT p.id AS parent_id, p.app_id,
       (SELECT COALESCE(MAX(id), 0) FROM upm_permission) AS base_id
FROM upm_permission p
JOIN upm_application a ON a.id = p.app_id AND a.tenant_id = 1 AND a.code = 'infra' AND a.deleted = 0
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.component = 'flow/todo/index';

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 1, parent_id, app_id, 1, '转办', 2, 'flow:task:transfer', 4, 'web', 1, 1, 0, 1
FROM migration_flow_transfer_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 2, parent_id, app_id, 1, '委派', 2, 'flow:task:delegate', 5, 'web', 1, 1, 0, 1
FROM migration_flow_transfer_ids;

INSERT INTO upm_role_permission(role_id, permission_id, tenant_id, device)
SELECT r.id, p.id, 1, 'web'
FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_flow_transfer_ids m
WHERE r.tenant_id = 1 AND r.id IN (10, 20) AND p.id BETWEEN m.base_id + 1 AND m.base_id + 2;

DROP TABLE migration_flow_transfer_ids;
