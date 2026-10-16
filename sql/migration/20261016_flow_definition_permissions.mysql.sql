-- Flow 定义管理权限。先确认 197~203 未被使用，再执行一次。
-- 菜单先停用，避免前端页面尚未接入时出现空路由；按钮权限仍可由后端鉴权。
-- 前端流程定义页面上线后，启用 197、198 两条菜单并补齐 198 的 component。
INSERT INTO upm_permission
    (id, parent_id, app_id, tenant_id, permission_name, permission_type, menu_url, menu_icon,
     sort_number, device, operable, usable, deleted, version)
VALUES
    (197, 0, 1, 1, 'OA审批', 0, 'flow', 'lucide:workflow', 2, 'web', 1, 0, 0, 1),
    (198, 197, 1, 1, '流程定义', 1, 'definition', 'lucide:git-branch', 1, 'web', 1, 0, 0, 1);

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
SELECT r.id, p.id, 1, 'web'
FROM upm_role r CROSS JOIN upm_permission p
WHERE r.id IN (10, 20) AND p.id BETWEEN 197 AND 203;
