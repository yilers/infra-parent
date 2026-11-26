-- 1.2.1 字段权限迁移。执行前备份并停止写入；可重复执行。
-- 旧列权限未启用，清理其配置表；已声明的新字段默认隐藏，内置管理员在本租户具有完整权限。
CREATE TABLE IF NOT EXISTS upm_field (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    resource_code VARCHAR(80) NOT NULL,
    resource_name VARCHAR(100) NOT NULL,
    field_code VARCHAR(80) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    mask_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    write_supported BOOLEAN NOT NULL DEFAULT FALSE,
    sort_number INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    usable SMALLINT NOT NULL DEFAULT 1,
    operable SMALLINT NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_id BIGINT,
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3)
);
CREATE INDEX IF NOT EXISTS idx_field_resource ON upm_field (tenant_id, resource_code, field_code);
CREATE TABLE IF NOT EXISTS upm_role_field (
    tenant_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    view_mode VARCHAR(20) NOT NULL,
    editable BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_role_field UNIQUE (tenant_id, role_id, field_id)
);
INSERT INTO upm_field (id,tenant_id,resource_code,resource_name,field_code,field_name,mask_type,write_supported)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_field)+ROW_NUMBER() OVER (ORDER BY t.id,d.code),
t.id,'system:user','用户资料',d.code,d.label,d.mask,TRUE
FROM upm_tenant t CROSS JOIN (SELECT 'phone' AS code, '手机号' AS label, 'PHONE' AS mask UNION ALL SELECT 'email', '邮箱', 'EMAIL') d
WHERE t.deleted=0
AND NOT EXISTS (SELECT 1 FROM upm_field f WHERE f.tenant_id=t.id
AND f.resource_code='system:user' AND f.field_code=d.code AND f.deleted=0);
INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY p.id),
p.id,p.app_id,p.tenant_id,'字段权限',2,'system:role:field',8,p.device,1,1,0,1
FROM upm_permission p WHERE p.component='system/role/index' AND p.deleted=0
AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.parent_id=p.id AND x.tenant_id=p.tenant_id
AND x.app_id=p.app_id AND x.permission_code='system:role:field' AND x.deleted=0);
INSERT INTO upm_role_permission (role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,r.tenant_id,p.device FROM upm_role r JOIN upm_permission p ON p.tenant_id=r.tenant_id
WHERE r.role_code IN ('platformAdmin','tenantAdmin') AND r.deleted=0
AND p.permission_code='system:role:field' AND p.deleted=0
AND NOT EXISTS (SELECT 1 FROM upm_role_permission x WHERE x.role_id=r.id AND x.permission_id=p.id);
DROP TABLE IF EXISTS upm_role_column;
-- 核验：目录包含本租户system:user的phone/email；角色管理有system:role:field按钮。重新登录后授权其他角色。

COMMENT ON TABLE upm_field IS '业务字段权限目录';
COMMENT ON COLUMN upm_field.id IS '字段目录ID';
COMMENT ON COLUMN upm_field.tenant_id IS '所属租户ID';
COMMENT ON COLUMN upm_field.resource_code IS '业务资源编码';
COMMENT ON COLUMN upm_field.resource_name IS '业务资源名称';
COMMENT ON COLUMN upm_field.field_code IS '业务字段编码';
COMMENT ON COLUMN upm_field.field_name IS '字段显示名称';
COMMENT ON COLUMN upm_field.mask_type IS '脱敏方式NONE/PHONE/EMAIL';
COMMENT ON COLUMN upm_field.write_supported IS '代码是否支持编辑权限';
COMMENT ON COLUMN upm_field.sort_number IS '排序';

COMMENT ON TABLE upm_role_field IS '角色字段授权';
COMMENT ON COLUMN upm_role_field.tenant_id IS '所属租户ID';
COMMENT ON COLUMN upm_role_field.role_id IS '角色ID';
COMMENT ON COLUMN upm_role_field.field_id IS '字段目录ID';
COMMENT ON COLUMN upm_role_field.view_mode IS '查看级别MASKED/FULL，无记录表示隐藏';
COMMENT ON COLUMN upm_role_field.editable IS '允许修改，须完整可见';
