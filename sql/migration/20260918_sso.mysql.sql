-- UPM SSO Server 应用配置增量脚本（MySQL 5.7+）。
-- 执行前请停止应用写入，执行完成后再发布对应版本服务。
ALTER TABLE upm_application
    ADD COLUMN logo VARCHAR(255) DEFAULT '' AFTER icon,
    ADD COLUMN home_url VARCHAR(500) DEFAULT '' AFTER logo,
    ADD COLUMN sso_enabled TINYINT NOT NULL DEFAULT 0 AFTER home_url,
    ADD COLUMN sso_secret VARCHAR(512) DEFAULT NULL AFTER sso_enabled,
    ADD COLUMN redirect_uris TEXT AFTER sso_secret,
    ADD COLUMN sso_push_url VARCHAR(500) DEFAULT '' AFTER redirect_uris,
    ADD COLUMN secret_update_time DATETIME(3) DEFAULT NULL AFTER sso_push_url;

-- 所有存量应用默认关闭SSO，配置并生成密钥后再逐个启用。
UPDATE upm_application SET sso_enabled = 0, sso_secret = NULL, secret_update_time = NULL;
