-- MySQL 5.7 增量迁移；先确认字段、索引不存在。已部分执行时只执行缺失的语句。
ALTER TABLE shortlink_link ADD COLUMN batch_id VARCHAR(128) NULL COMMENT '批次标识，单条创建为空';
ALTER TABLE shortlink_link ADD COLUMN business_id VARCHAR(128) NULL COMMENT '批次内业务标识';
ALTER TABLE shortlink_link ADD INDEX idx_shortlink_link_batch (tenant_id, config_id, batch_id);
CREATE TABLE IF NOT EXISTS shortlink_batch (
    id BIGINT NOT NULL PRIMARY KEY, config_id BIGINT NOT NULL, batch_id VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL, item_count INT NOT NULL, tenant_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1, usable INT NOT NULL DEFAULT 1, operable INT NOT NULL DEFAULT 1,
    deleted INT NOT NULL DEFAULT 0, create_id BIGINT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_shortlink_batch (tenant_id, config_id, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='短链批次幂等记录';
