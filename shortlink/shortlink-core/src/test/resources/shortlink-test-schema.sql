CREATE TABLE shortlink_config (id BIGINT PRIMARY KEY, name VARCHAR(100), code VARCHAR(64), target_type VARCHAR(16),
 url_template VARCHAR(2048), default_valid_seconds BIGINT, tenant_id BIGINT, version INT, usable INT, operable INT,
 deleted INT, create_id BIGINT, create_time TIMESTAMP, update_time TIMESTAMP, UNIQUE(tenant_id,code));
CREATE TABLE shortlink_domain (id BIGINT PRIMARY KEY, host VARCHAR(253), scheme VARCHAR(8), public_port INT, tenant_id BIGINT,
 version INT, usable INT, operable INT, deleted INT, create_id BIGINT, create_time TIMESTAMP, update_time TIMESTAMP, UNIQUE(host));
CREATE TABLE shortlink_namespace (id BIGINT PRIMARY KEY, config_id BIGINT, domain_id BIGINT, path_code VARCHAR(32),
 generate_type VARCHAR(16), code_length INT, code_algorithm VARCHAR(16) DEFAULT 'SEQUENTIAL', code_key_cipher VARCHAR(128),
 tenant_id BIGINT, version INT, usable INT, operable INT, deleted INT,
 create_id BIGINT, create_time TIMESTAMP, update_time TIMESTAMP, UNIQUE(domain_id,path_code));
CREATE TABLE shortlink_sequence (namespace_id BIGINT PRIMARY KEY, tenant_id BIGINT, max_allocated_id BIGINT, step INT, update_time TIMESTAMP);
CREATE TABLE shortlink_config_grant (id BIGINT PRIMARY KEY, config_id BIGINT, credential_id BIGINT, tenant_id BIGINT,
 version INT, usable INT, operable INT, deleted INT, create_id BIGINT, create_time TIMESTAMP, update_time TIMESTAMP,
 UNIQUE(tenant_id,config_id,credential_id));
CREATE TABLE shortlink_link (id BIGINT PRIMARY KEY, config_id BIGINT, namespace_id BIGINT, short_code VARCHAR(8),
 target_url VARCHAR(4096), business_code VARCHAR(512), batch_id VARCHAR(128), business_id VARCHAR(128), request_id VARCHAR(128), request_hash VARCHAR(64), expire_time TIMESTAMP,
 credential_id BIGINT, tenant_id BIGINT, version INT, usable INT, operable INT, deleted INT, create_id BIGINT,
 create_time TIMESTAMP, update_time TIMESTAMP, UNIQUE(namespace_id,short_code), UNIQUE(tenant_id,config_id,request_id));
CREATE TABLE shortlink_batch (id BIGINT PRIMARY KEY, config_id BIGINT, batch_id VARCHAR(128), request_hash VARCHAR(64),
 item_count INT, tenant_id BIGINT, version INT, usable INT, operable INT, deleted INT, create_id BIGINT,
 create_time TIMESTAMP, update_time TIMESTAMP, UNIQUE(tenant_id, config_id, batch_id));
