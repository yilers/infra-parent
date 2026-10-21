-- 自动抄送：先停写并备份；已完成 Flow 和请假菜单迁移的库只执行一次。
-- 先确认 flow_cc 不存在，租户1 infra 应用下恰有一个 OA 目录，且无 flow/cc/index 菜单。
CREATE TABLE flow_cc (
    id BIGINT NOT NULL COMMENT '雪花ID',
    tenant_id VARCHAR(40) NOT NULL COMMENT '租户ID，与Warm-Flow原生表一致',
    instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    source_task_id BIGINT NOT NULL COMMENT '触发抄送的审批任务ID',
    node_code VARCHAR(100) NOT NULL COMMENT '触发节点编码快照',
    node_name VARCHAR(200) NOT NULL COMMENT '触发节点名称快照',
    recipient_id VARCHAR(40) NOT NULL COMMENT '实际接收用户ID',
    recipient_name VARCHAR(128) NOT NULL COMMENT '实际接收用户姓名快照',
    create_time DATETIME(3) NOT NULL COMMENT '抄送时间',
    read_time DATETIME(3) DEFAULT NULL COMMENT '首次查看时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_cc_task_recipient (tenant_id, source_task_id, recipient_id),
    KEY idx_flow_cc_inbox (tenant_id, recipient_id, create_time, id),
    KEY idx_flow_cc_instance (tenant_id, instance_id)
) COMMENT='流程节点自动抄送接收记录';

CREATE TEMPORARY TABLE migration_flow_cc_ids AS
SELECT p.id AS parent_id, p.app_id,
       (SELECT COALESCE(MAX(id), 0) FROM upm_permission) AS base_id
FROM upm_permission p
JOIN upm_application a ON a.id=p.app_id AND a.tenant_id=1 AND a.code='infra' AND a.deleted=0
WHERE p.tenant_id=1 AND p.deleted=0 AND p.permission_type=0 AND p.menu_url='flow';

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,sort_number,device,operable,usable,deleted,version)
SELECT base_id+1,parent_id,app_id,1,'抄送我的',1,'cc','lucide:mail-check','flow/cc/index',5,'web',1,1,0,1
FROM migration_flow_cc_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+2,base_id+1,app_id,1,'查询',2,'flow:cc:list',1,'web',1,1,0,1
FROM migration_flow_cc_ids;

INSERT INTO upm_role_permission(role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,1,'web' FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_flow_cc_ids m
WHERE r.tenant_id=1 AND r.id IN (10,20) AND p.id BETWEEN m.base_id+1 AND m.base_id+2;

UPDATE upm_permission p SET sort_number=6
WHERE p.tenant_id=1 AND p.deleted=0 AND p.component='flow/leave/index'
  AND EXISTS (SELECT 1 FROM migration_flow_cc_ids m
              WHERE p.parent_id=m.parent_id AND p.app_id=m.app_id);
DROP TABLE migration_flow_cc_ids;
