-- 请假申请：仅执行一次，前置已执行20261019_flow_inbox和20261019_flow_task_actions。
-- 必须停写、备份；确认尚无flow_ext_leave表、flow/leave/index菜单和flow:leave:*权限，
-- 且模板租户1的infra应用下恰有一个OA审批目录。其他租户随后同步菜单、授权并重新登录。
-- 不创建、复制或修改任何流程定义；提交时使用当前租户已发布的leave流程。
-- 请假示例业务表；状态实时关联引擎实例，不重复存储。
CREATE TABLE flow_ext_leave (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(40) NOT NULL,
    applicant_id VARCHAR(64) NOT NULL,
    applicant_name VARCHAR(100) NOT NULL,
    leave_type VARCHAR(20) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    duration_hours NUMERIC(8,1) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    flow_code VARCHAR(40) NOT NULL,
    instance_id BIGINT,
    version INTEGER NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flow_leave_instance UNIQUE (tenant_id, instance_id)
);
CREATE INDEX idx_flow_leave_mine ON flow_ext_leave(tenant_id, applicant_id, create_time, id);
COMMENT ON TABLE flow_ext_leave IS '请假申请业务表';
COMMENT ON COLUMN flow_ext_leave.id IS '请假申请ID';
COMMENT ON COLUMN flow_ext_leave.tenant_id IS '租户ID，与Flow引擎保持字符串类型';
COMMENT ON COLUMN flow_ext_leave.applicant_id IS '申请人用户ID';
COMMENT ON COLUMN flow_ext_leave.applicant_name IS '申请人姓名快照';
COMMENT ON COLUMN flow_ext_leave.leave_type IS 'PERSONAL事假 SICK病假 ANNUAL年假 COMP_TIME调休 OTHER其他';
COMMENT ON COLUMN flow_ext_leave.start_time IS '请假开始时间';
COMMENT ON COLUMN flow_ext_leave.end_time IS '请假结束时间';
COMMENT ON COLUMN flow_ext_leave.duration_hours IS '请假小时数，0.5的整数倍，不自动计算排班';
COMMENT ON COLUMN flow_ext_leave.reason IS '请假原因，不放入流程变量';
COMMENT ON COLUMN flow_ext_leave.flow_code IS '绑定流程编码，默认由服务端配置为leave';
COMMENT ON COLUMN flow_ext_leave.instance_id IS '流程实例ID，空值表示草稿';
COMMENT ON COLUMN flow_ext_leave.version IS '乐观锁版本';
COMMENT ON COLUMN flow_ext_leave.create_time IS '创建时间';
COMMENT ON COLUMN flow_ext_leave.update_time IS '最后编辑或提交时间';

-- 前四项按流程定义、我的待办、我的已办、我发起的排列；不改变是否可操作和授权状态。
UPDATE upm_permission SET sort_number = CASE component
    WHEN 'flow/definition/index' THEN 1 WHEN 'flow/todo/index' THEN 2
    WHEN 'flow/done/index' THEN 3 WHEN 'flow/started/index' THEN 4 END,
    version = version + 1
WHERE deleted = 0 AND permission_type = 1
  AND component IN ('flow/definition/index', 'flow/todo/index', 'flow/done/index', 'flow/started/index')
  AND EXISTS (SELECT 1 FROM upm_application a
              WHERE a.id = upm_permission.app_id AND a.tenant_id = upm_permission.tenant_id
                AND a.code = 'infra' AND a.deleted = 0);

-- 菜单ID使用当前最大值后的连续6个ID；每条INSERT只读取一次临时表，兼容MySQL 5.7。
CREATE TEMPORARY TABLE migration_flow_leave_ids AS
SELECT p.id AS parent_id, p.app_id,
       (SELECT COALESCE(MAX(id), 0) FROM upm_permission) AS base_id
FROM upm_permission p
JOIN upm_application a ON a.id = p.app_id AND a.tenant_id = 1 AND a.code = 'infra' AND a.deleted = 0
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.permission_type = 0 AND p.menu_url = 'flow';

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,sort_number,device,operable,usable,deleted,version)
SELECT base_id+1,parent_id,app_id,1,'请假申请',1,'leave','lucide:calendar-days','flow/leave/index',5,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+2,base_id+1,app_id,1,'查询',2,'flow:leave:list',1,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+3,base_id+1,app_id,1,'新增',2,'flow:leave:add',2,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+4,base_id+1,app_id,1,'编辑草稿',2,'flow:leave:edit',3,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+5,base_id+1,app_id,1,'提交审批',2,'flow:leave:submit',4,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+6,base_id+1,app_id,1,'删除草稿',2,'flow:leave:delete',5,'web',1,1,0,1
FROM migration_flow_leave_ids;

INSERT INTO upm_role_permission(role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,1,'web'
FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_flow_leave_ids m
WHERE r.tenant_id=1 AND r.id IN (10,20) AND p.id BETWEEN m.base_id+1 AND m.base_id+6;
DROP TABLE migration_flow_leave_ids;
