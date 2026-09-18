-- UPM SSO Server 应用配置增量脚本（PostgreSQL）。
-- 执行前请停止应用写入，执行完成后再发布对应版本服务。
ALTER TABLE upm_application
    ADD COLUMN logo VARCHAR(255) DEFAULT '',
    ADD COLUMN home_url VARCHAR(500) DEFAULT '',
    ADD COLUMN sso_enabled SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN sso_secret VARCHAR(512) DEFAULT NULL,
    ADD COLUMN redirect_uris TEXT,
    ADD COLUMN sso_push_url VARCHAR(500) DEFAULT '',
    ADD COLUMN secret_update_time TIMESTAMP(3) DEFAULT NULL;

UPDATE upm_application SET sso_enabled = 0, sso_secret = NULL, secret_update_time = NULL;
