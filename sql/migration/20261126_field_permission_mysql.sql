-- 1.2.1 字段权限迁移。执行前备份并停止写入；可重复执行。
-- 旧列权限未启用，清理其配置表；已声明的新字段默认隐藏，内置管理员在本租户具有完整权限。
CREATE TABLE IF NOT EXISTS upm_field (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '字段目录ID',
    tenant_id BIGINT NOT NULL COMMENT '所属租户ID',
    resource_code VARCHAR(80) NOT NULL COMMENT '业务资源编码',
    resource_name VARCHAR(100) NOT NULL COMMENT '业务资源名称',
    field_code VARCHAR(80) NOT NULL COMMENT '业务字段编码',
    field_name VARCHAR(100) NOT NULL COMMENT '字段显示名称',
    mask_type VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '脱敏方式NONE/PHONE/EMAIL',
    write_supported BOOLEAN NOT NULL DEFAULT FALSE COMMENT '代码是否支持编辑权限',
    sort_number INT NOT NULL DEFAULT 0 COMMENT '排序',
    version INT NOT NULL DEFAULT 1,
    usable SMALLINT NOT NULL DEFAULT 1,
    operable SMALLINT NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_id BIGINT,
    create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_field_resource (tenant_id, resource_code, field_code)
);
CREATE TABLE IF NOT EXISTS upm_role_field (
    tenant_id BIGINT NOT NULL COMMENT '所属租户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    field_id BIGINT NOT NULL COMMENT '字段目录ID',
    view_mode VARCHAR(20) NOT NULL COMMENT '查看级别MASKED/FULL，无记录表示隐藏',
    editable BOOLEAN NOT NULL DEFAULT FALSE COMMENT '允许修改，须完整可见',
    CONSTRAINT uk_role_field UNIQUE (tenant_id, role_id, field_id)
);
SET @field_next=(SELECT COALESCE(MAX(id),0) FROM upm_field);
INSERT INTO upm_field (id,tenant_id,resource_code,resource_name,field_code,field_name,mask_type,write_supported)
SELECT (@field_next:=@field_next+1),t.id,'system:user','用户资料',d.code,d.label,d.mask,TRUE
FROM upm_tenant t CROSS JOIN (SELECT 'phone' AS code, '手机号' AS label, 'PHONE' AS mask UNION ALL SELECT 'email', '邮箱', 'EMAIL') d
WHERE t.deleted=0
AND NOT EXISTS (SELECT 1 FROM upm_field f WHERE f.tenant_id=t.id
AND f.resource_code='system:user' AND f.field_code=d.code AND f.deleted=0) ORDER BY t.id,d.code;
SET @field_menu_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);
INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@field_menu_next:=@field_menu_next+1),p.id,p.app_id,p.tenant_id,'字段权限',2,'system:role:field',8,p.device,1,1,0,1
FROM upm_permission p WHERE p.component='system/role/index' AND p.deleted=0
AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.parent_id=p.id AND x.tenant_id=p.tenant_id
AND x.app_id=p.app_id AND x.permission_code='system:role:field' AND x.deleted=0) ORDER BY p.id;
INSERT INTO upm_role_permission (role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,r.tenant_id,p.device FROM upm_role r JOIN upm_permission p ON p.tenant_id=r.tenant_id
WHERE r.role_code IN ('platformAdmin','tenantAdmin') AND r.deleted=0
AND p.permission_code='system:role:field' AND p.deleted=0
AND NOT EXISTS (SELECT 1 FROM upm_role_permission x WHERE x.role_id=r.id AND x.permission_id=p.id);
DROP TABLE IF EXISTS upm_role_column;
-- 核验：目录包含本租户system:user的phone/email；角色管理有system:role:field按钮。重新登录后授权其他角色。
