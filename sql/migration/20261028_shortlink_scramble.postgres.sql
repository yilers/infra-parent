-- PostgreSQL：先执行20261028_shortlink，备份并停止短链创建后执行。
-- 本脚本不修改现有路径、号段或短链；旧路径保留SEQUENTIAL算法。
ALTER TABLE shortlink_namespace ADD COLUMN IF NOT EXISTS
    code_algorithm VARCHAR(16) NOT NULL DEFAULT 'SEQUENTIAL';
ALTER TABLE shortlink_namespace ADD COLUMN IF NOT EXISTS
    code_key_cipher VARCHAR(128) NULL;
COMMENT ON COLUMN shortlink_namespace.code_algorithm IS '短码算法：SEQUENTIAL旧版顺序，FF1_V1打散，NONE自定义';
COMMENT ON COLUMN shortlink_namespace.code_key_cipher IS 'namespace打散密钥密文，创建后不可更改';
