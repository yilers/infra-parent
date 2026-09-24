-- PostgreSQL 第三方账号绑定约束增量迁移。停服、备份后执行一次，不可重复执行。
-- 前置条件：已经执行20260923_third_auth.postgres.sql，并确认以下重复检查均无结果。
BEGIN;

-- 兼容部分早期数据库缺少逻辑删除和审计字段的情况。
ALTER TABLE upm_user_third
    ADD COLUMN IF NOT EXISTS deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE upm_user_third
    ADD COLUMN IF NOT EXISTS create_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);
ALTER TABLE upm_user_third
    ADD COLUMN IF NOT EXISTS update_time TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3);

-- 同一用户同一平台不得存在多条绑定。
SELECT tenant_id, user_id, platform, COUNT(*) AS duplicate_count
FROM upm_user_third
GROUP BY tenant_id, user_id, platform
HAVING COUNT(*) > 1;

-- 同一租户内，同一平台OpenId和非空UnionId不得绑定多个用户。
SELECT tenant_id, platform, open_id, COUNT(*) AS duplicate_count
FROM upm_user_third
WHERE open_id IS NOT NULL AND open_id <> ''
GROUP BY tenant_id, platform, open_id
HAVING COUNT(*) > 1;

SELECT tenant_id, platform, union_id, COUNT(*) AS duplicate_count
FROM upm_user_third
WHERE union_id IS NOT NULL AND union_id <> ''
GROUP BY tenant_id, platform, union_id
HAVING COUNT(*) > 1;

UPDATE upm_user_third SET open_id = NULL WHERE open_id = '';
UPDATE upm_user_third SET union_id = NULL WHERE union_id = '';

-- 清理旧版缺少租户和平台维度的全局OpenId唯一索引。
DROP INDEX IF EXISTS uk_open_id;

ALTER TABLE upm_user_third ALTER COLUMN open_id TYPE VARCHAR(128);
ALTER TABLE upm_user_third ALTER COLUMN union_id TYPE VARCHAR(128);
ALTER TABLE upm_user_third ALTER COLUMN platform TYPE VARCHAR(30);
ALTER TABLE upm_user_third ALTER COLUMN session_key TYPE VARCHAR(100);
ALTER TABLE upm_user_third ALTER COLUMN expand TYPE VARCHAR(1000);
ALTER TABLE upm_user_third ALTER COLUMN open_id DROP DEFAULT;
ALTER TABLE upm_user_third ALTER COLUMN union_id DROP DEFAULT;
ALTER TABLE upm_user_third ADD CONSTRAINT uk_user_third_user_platform
    UNIQUE (tenant_id, user_id, platform);
ALTER TABLE upm_user_third ADD CONSTRAINT uk_user_third_open_platform
    UNIQUE (tenant_id, platform, open_id);
CREATE UNIQUE INDEX uk_user_third_union_platform
    ON upm_user_third (tenant_id, platform, union_id)
    WHERE union_id IS NOT NULL AND union_id <> '';

COMMIT;
