-- MySQL 5.7 / 8.x 部门主管功能迁移。停写、备份后执行一次。
-- upm_dept_leader 在 01_mysql_init.sql 中已创建；本迁移仅补索引和权限。
ALTER TABLE upm_dept_leader
    MODIFY COLUMN leader_type TINYINT NOT NULL DEFAULT 1 COMMENT '主管类型 1-主管 2-副主管',
    MODIFY COLUMN end_date DATE DEFAULT NULL COMMENT '任职结束日期（不包含当天）, NULL表示仍在任',
    ADD INDEX idx_dept_leader_dept (tenant_id, dept_id, end_date),
    ADD INDEX idx_dept_leader_user (tenant_id, user_id, end_date);

-- 模板租户新增可授权按钮；其他租户通过“同步应用菜单”获取。
INSERT INTO upm_permission
    (id, parent_id, sort_number, permission_code, permission_name, permission_type,
     operable, usable, deleted, tenant_id, version, create_time, update_time,
     component, cache, link, device, create_id, app_id)
VALUES
    (196, 130, 5, 'system:dept:leader:edit', '设置主管', 2,
     1, 1, 0, 1, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3),
     '', 0, 0, 'web', 1, 1);

INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device)
VALUES (10, 196, 1, 'web');
INSERT INTO upm_role_permission (role_id, permission_id, tenant_id, device)
VALUES (20, 196, 1, 'web');
