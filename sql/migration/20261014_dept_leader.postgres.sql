-- PostgreSQL 部门主管功能迁移。停写、备份后执行一次。
-- upm_dept_leader 在 01_postgres_init.sql 中已创建；本迁移仅补索引和权限。
COMMENT ON COLUMN upm_dept_leader.leader_type IS '主管类型 1-主管 2-副主管';
COMMENT ON COLUMN upm_dept_leader.end_date IS '任职结束日期（不包含当天）, NULL表示仍在任';
CREATE INDEX idx_dept_leader_dept ON upm_dept_leader (tenant_id, dept_id, end_date);
CREATE INDEX idx_dept_leader_user ON upm_dept_leader (tenant_id, user_id, end_date);

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
