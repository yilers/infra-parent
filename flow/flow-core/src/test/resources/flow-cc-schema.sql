CREATE TABLE flow_cc (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(40) NOT NULL,
    instance_id BIGINT NOT NULL,
    source_task_id BIGINT NOT NULL,
    node_code VARCHAR(100) NOT NULL,
    node_name VARCHAR(200) NOT NULL,
    recipient_id VARCHAR(40) NOT NULL,
    recipient_name VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    read_time TIMESTAMP,
    CONSTRAINT uk_flow_cc_task_recipient UNIQUE (tenant_id, source_task_id, recipient_id)
);
