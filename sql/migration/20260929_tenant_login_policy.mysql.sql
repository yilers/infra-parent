-- MySQL 5.7 / 8.x 租户账号密码登录策略增量迁移。
-- 停服、备份后执行一次，不可重复执行；无策略记录表示该租户不限制密码失败次数。
CREATE TABLE upm_tenant_login_policy (
    id BIGINT NOT NULL COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    max_failures INT NOT NULL DEFAULT 3 COMMENT '统计周期内最大连续失败次数',
    failure_window_minutes INT NOT NULL DEFAULT 10 COMMENT '失败统计周期，单位分钟',
    lock_duration_minutes INT NOT NULL DEFAULT 10 COMMENT '账号锁定时长，单位分钟',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_tenant_login_policy_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='租户账号密码登录策略表';

-- 存量有效租户默认启用：10分钟内连续失败3次，锁定10分钟。
-- 初始策略使用tenant_id作为本表主键；后续新建租户使用雪花ID，两者不会冲突。
INSERT INTO upm_tenant_login_policy
    (id, tenant_id, max_failures, failure_window_minutes, lock_duration_minutes, version)
SELECT id, id, 3, 10, 10, 1
FROM upm_tenant
WHERE deleted = 0;
