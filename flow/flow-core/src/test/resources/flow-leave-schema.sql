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
