-- PostgreSQL 租户账号密码登录策略增量迁移。
-- 停服、备份后执行一次，不可重复执行；无策略记录表示该租户不限制密码失败次数。
CREATE TABLE upm_tenant_login_policy (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    max_failures INT NOT NULL DEFAULT 3,
    failure_window_minutes INT NOT NULL DEFAULT 10,
    lock_duration_minutes INT NOT NULL DEFAULT 10,
    version INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_tenant_login_policy_tenant UNIQUE (tenant_id)
);
COMMENT ON TABLE upm_tenant_login_policy IS '租户账号密码登录策略表';
COMMENT ON COLUMN upm_tenant_login_policy.tenant_id IS '租户ID';
COMMENT ON COLUMN upm_tenant_login_policy.max_failures IS '统计周期内最大连续失败次数';
COMMENT ON COLUMN upm_tenant_login_policy.failure_window_minutes IS '失败统计周期，单位分钟';
COMMENT ON COLUMN upm_tenant_login_policy.lock_duration_minutes IS '账号锁定时长，单位分钟';
COMMENT ON COLUMN upm_tenant_login_policy.version IS '乐观锁版本号';

CREATE OR REPLACE FUNCTION update_upm_tenant_login_policy_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP(3);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_upm_tenant_login_policy_update_time
    BEFORE UPDATE ON upm_tenant_login_policy
    FOR EACH ROW
    EXECUTE FUNCTION update_upm_tenant_login_policy_update_time();

-- 存量有效租户默认启用：10分钟内连续失败3次，锁定10分钟。
-- 初始策略使用tenant_id作为本表主键；后续新建租户使用雪花ID，两者不会冲突。
INSERT INTO upm_tenant_login_policy
    (id, tenant_id, max_failures, failure_window_minutes, lock_duration_minutes, version)
SELECT id, id, 3, 10, 10, 1
FROM upm_tenant
WHERE deleted = 0;
