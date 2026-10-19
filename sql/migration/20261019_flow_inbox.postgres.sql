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
SELECT base_id + 1, parent_id, app_id, 1, '我的待办', 1, 'todo', 'lucide:inbox', 'flow/todo/index', 2, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids
UNION ALL
SELECT base_id + 2, parent_id, app_id, 1, '我的已办', 1, 'done', 'lucide:circle-check', 'flow/done/index', 3, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids
UNION ALL
SELECT base_id + 3, parent_id, app_id, 1, '我发起的', 1, 'started', 'lucide:send', 'flow/started/index', 4, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids;

INSERT INTO upm_permission
(id, parent_id, app_id, tenant_id, permission_name, permission_type, permission_code,
 sort_number, device, operable, usable, deleted, version)
SELECT base_id + 4, base_id + 1, app_id, 1, '查询', 2, 'flow:task:todo', 1, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids
UNION ALL
SELECT base_id + 5, base_id + 2, app_id, 1, '查询', 2, 'flow:task:done', 1, 'web', 1, 1, 0, 1 FROM migration_flow_inbox_ids
UNION ALL
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
