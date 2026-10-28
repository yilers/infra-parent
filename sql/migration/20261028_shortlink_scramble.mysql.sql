-- MySQL 5.7/8.x：先执行20261028_shortlink，备份并停止短链创建后执行。
-- 本脚本不修改现有路径、号段或短链；旧路径保留SEQUENTIAL算法。
-- MySQL 5.7不支持ADD COLUMN IF NOT EXISTS；如部分执行，先核验列，只执行尚缺的语句。
ALTER TABLE shortlink_namespace
    ADD COLUMN code_algorithm VARCHAR(16) NOT NULL DEFAULT 'SEQUENTIAL'
    COMMENT '短码算法：SEQUENTIAL旧版顺序，FF1_V1打散，NONE自定义';

ALTER TABLE shortlink_namespace
    ADD COLUMN code_key_cipher VARCHAR(128) NULL
    COMMENT 'namespace打散密钥密文，创建后不可更改';
