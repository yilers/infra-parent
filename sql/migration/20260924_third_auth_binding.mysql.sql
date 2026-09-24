-- MySQL 5.7 / 8.x 第三方账号绑定约束增量迁移。停服、备份后执行一次，不可重复执行。
-- 前置条件：已经执行20260923_third_auth.mysql.sql，并确认以下重复检查均无结果。

-- 兼容部分早期数据库缺少逻辑删除和审计字段的情况。MySQL 5.7不支持ADD COLUMN IF NOT EXISTS，
-- 因此通过information_schema分别判断后再执行，已有字段时不会重复添加。
SET @user_third_has_deleted = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'upm_user_third'
      AND COLUMN_NAME = 'deleted'
);
SET @user_third_add_deleted_sql = IF(
    @user_third_has_deleted = 0,
    'ALTER TABLE upm_user_third ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''是否删除 1-是 0-否'' AFTER tenant_id',
    'SELECT 1'
);
PREPARE user_third_add_deleted_statement FROM @user_third_add_deleted_sql;
EXECUTE user_third_add_deleted_statement;
DEALLOCATE PREPARE user_third_add_deleted_statement;

SET @user_third_has_create_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'upm_user_third'
      AND COLUMN_NAME = 'create_time'
);
SET @user_third_add_create_time_sql = IF(
    @user_third_has_create_time = 0,
    'ALTER TABLE upm_user_third ADD COLUMN create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT ''创建时间'' AFTER deleted',
    'SELECT 1'
);
PREPARE user_third_add_create_time_statement FROM @user_third_add_create_time_sql;
EXECUTE user_third_add_create_time_statement;
DEALLOCATE PREPARE user_third_add_create_time_statement;

SET @user_third_has_update_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'upm_user_third'
      AND COLUMN_NAME = 'update_time'
);
SET @user_third_add_update_time_sql = IF(
    @user_third_has_update_time = 0,
    'ALTER TABLE upm_user_third ADD COLUMN update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT ''修改时间'' AFTER create_time',
    'SELECT 1'
);
PREPARE user_third_add_update_time_statement FROM @user_third_add_update_time_sql;
EXECUTE user_third_add_update_time_statement;
DEALLOCATE PREPARE user_third_add_update_time_statement;

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

-- 旧表的空字符串不代表真实平台身份，先转为NULL，允许多名未绑定用户共存。
UPDATE upm_user_third SET open_id = NULL WHERE open_id = '';
UPDATE upm_user_third SET union_id = NULL WHERE union_id = '';

-- 旧版全局OpenId唯一索引没有租户和平台维度，会误阻止合法绑定。
SET @user_third_has_legacy_open_index = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'upm_user_third'
      AND INDEX_NAME = 'uk_open_id'
);
SET @user_third_drop_legacy_open_index_sql = IF(
    @user_third_has_legacy_open_index > 0,
    'ALTER TABLE upm_user_third DROP INDEX uk_open_id',
    'SELECT 1'
);
PREPARE user_third_drop_legacy_open_index_statement FROM @user_third_drop_legacy_open_index_sql;
EXECUTE user_third_drop_legacy_open_index_statement;
DEALLOCATE PREPARE user_third_drop_legacy_open_index_statement;

ALTER TABLE upm_user_third
    MODIFY COLUMN platform VARCHAR(30) DEFAULT 'wx' COMMENT '平台',
    MODIFY COLUMN open_id VARCHAR(128) DEFAULT NULL COMMENT 'openId',
    MODIFY COLUMN union_id VARCHAR(128) DEFAULT NULL COMMENT 'unionId',
    MODIFY COLUMN session_key VARCHAR(100) DEFAULT '' COMMENT 'sessionKey',
    MODIFY COLUMN expand VARCHAR(1000) DEFAULT '' COMMENT '扩展字段',
    ADD UNIQUE INDEX uk_user_third_user_platform (tenant_id, user_id, platform),
    ADD UNIQUE INDEX uk_user_third_open_platform (tenant_id, platform, open_id);

-- MySQL 5.7不支持带条件的唯一索引；非空UnionId重复由业务层校验，OpenId唯一索引负责并发兜底。
