-- 申请人拿回、撤销按钮。仅执行一次，前置已执行审批中心菜单及抄送迁移。
-- 停写、备份，确认模板租户1恰有一个flow/started/index菜单，且两个权限编码均不存在。
-- ID从当前最大值后连续分配；其他租户同步应用菜单后按需授权。
CREATE TEMPORARY TABLE migration_flow_instance_action_ids AS
SELECT p.id AS parent_id, p.app_id,
       (SELECT COALESCE(MAX(id), 0) FROM upm_permission) AS base_id
FROM upm_permission p
JOIN upm_application a ON a.id = p.app_id AND a.tenant_id = 1 AND a.code = 'infra' AND a.deleted = 0
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.component = 'flow/started/index';

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 1, parent_id, app_id, 1, '拿回', 2, 'flow:instance:take-back', 2, 'web', 1, 1, 0, 1
FROM migration_flow_instance_action_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 2, parent_id, app_id, 1, '撤销', 2, 'flow:instance:revoke', 3, 'web', 1, 1, 0, 1
FROM migration_flow_instance_action_ids;

INSERT INTO upm_role_permission(role_id, permission_id, tenant_id, device)
SELECT r.id, p.id, 1, 'web'
FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_flow_instance_action_ids m
WHERE r.tenant_id = 1 AND r.id IN (10, 20) AND p.id BETWEEN m.base_id + 1 AND m.base_id + 2;

DROP TABLE migration_flow_instance_action_ids;
