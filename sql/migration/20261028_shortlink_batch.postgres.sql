ALTER TABLE shortlink_link ADD COLUMN IF NOT EXISTS batch_id VARCHAR(128) NULL;
ALTER TABLE shortlink_link ADD COLUMN IF NOT EXISTS business_id VARCHAR(128) NULL;
CREATE INDEX IF NOT EXISTS idx_shortlink_link_batch ON shortlink_link (tenant_id, config_id, batch_id);
CREATE TABLE IF NOT EXISTS shortlink_batch (
    id BIGINT PRIMARY KEY, config_id BIGINT NOT NULL, batch_id VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL, item_count INT NOT NULL, tenant_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1, usable INT NOT NULL DEFAULT 1, operable INT NOT NULL DEFAULT 1,
    deleted INT NOT NULL DEFAULT 0, create_id BIGINT NULL,
    create_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE (tenant_id, config_id, batch_id)
);
COMMENT ON TABLE shortlink_batch IS '短链批次幂等记录';
COMMENT ON COLUMN shortlink_link.batch_id IS '批次标识，单条创建为空';
COMMENT ON COLUMN shortlink_link.business_id IS '批次内业务标识';
