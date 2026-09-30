-- PostgreSQL 用户批量新增权限增量迁移。
-- 停服、备份后执行一次，不可重复执行。
INSERT INTO upm_permission
    (id, parent_id, sort_number, permission_code, permission_name, permission_type,
     operable, usable, deleted, tenant_id, version, create_time, update_time,
     component, cache, link, device, create_id, app_id)
VALUES
    (118, 110, 8, 'system:user:batch:add', '批量新增', 2,
     1, 1, 0, 1, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3),
     '', 0, 0, 'web', 1, 1);

INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device)
VALUES (10, 118, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device)
VALUES (20, 118, 1, 'web');

-- 其他租户通过租户管理中的“同步应用菜单”获得该按钮，再由租户管理员按需分配给角色。
