-- 系统调用凭证与短链基础表。MySQL 5.7+/PostgreSQL。
-- 菜单脚本在文件末尾；全量数据库不要重复执行本增量。
-- 仅建表，不创建凭证、不复制密钥、不修改已有数据。执行前备份。
-- IF NOT EXISTS 仅支持完整建表后的重跑，不会修复已有表的字段差异。

CREATE TABLE IF NOT EXISTS upm_api_credential (
    id BIGINT NOT NULL COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '凭证名称',
    access_key VARCHAR(64) NOT NULL COMMENT '全局唯一的公开调用标识',
    secret_cipher VARCHAR(512) NOT NULL COMMENT '加密保存的调用密钥，禁止向列表和日志输出',
    expire_time DATETIME(3) NULL COMMENT '失效时间，空表示长期有效',
    rate_limit INT NOT NULL DEFAULT 600 COMMENT '每分钟允许调用次数',
    tenant_id BIGINT NOT NULL COMMENT '所属租户，必须来自可信上下文',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    usable INT NOT NULL DEFAULT 1 COMMENT '是否启用，1启用0停用',
    operable INT NOT NULL DEFAULT 1 COMMENT '是否允许操作',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除，删除不释放唯一标识',
    create_id BIGINT NULL COMMENT '用户创建人，系统调用为空',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间，由应用更新',
    PRIMARY KEY (id),
    UNIQUE (access_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='系统调用凭证';

CREATE TABLE IF NOT EXISTS upm_api_credential_permission (
    id BIGINT NOT NULL COMMENT '主键ID',
    credential_id BIGINT NOT NULL COMMENT '调用凭证ID',
    permission_code VARCHAR(128) NOT NULL COMMENT '接口能力编码，不依赖接口路径',
    tenant_id BIGINT NOT NULL COMMENT '所属租户，必须来自可信上下文',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    usable INT NOT NULL DEFAULT 1 COMMENT '是否启用，1启用0停用',
    operable INT NOT NULL DEFAULT 1 COMMENT '是否允许操作',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除，删除不释放唯一标识',
    create_id BIGINT NULL COMMENT '用户创建人，系统调用为空',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间，由应用更新',
    PRIMARY KEY (id),
    UNIQUE (tenant_id, credential_id, permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='系统调用接口授权';

CREATE TABLE IF NOT EXISTS shortlink_config (
    id BIGINT NOT NULL COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '配置名称',
    code VARCHAR(64) NOT NULL COMMENT '租户内唯一配置编码',
    target_type VARCHAR(16) NOT NULL COMMENT 'URL完整链接或TEMPLATE业务编码模板',
    url_template VARCHAR(2048) NULL COMMENT '模板必须包含{code}，只允许HTTP或HTTPS',
    default_valid_seconds BIGINT NULL COMMENT '默认有效秒数，空表示永久',
    tenant_id BIGINT NOT NULL COMMENT '所属租户，必须来自可信上下文',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    usable INT NOT NULL DEFAULT 1 COMMENT '是否启用，1启用0停用',
    operable INT NOT NULL DEFAULT 1 COMMENT '是否允许操作',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除，删除不释放唯一标识',
    create_id BIGINT NULL COMMENT '用户创建人，系统调用为空',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间，由应用更新',
    PRIMARY KEY (id),
    UNIQUE (tenant_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='短链配置';

CREATE TABLE IF NOT EXISTS shortlink_domain (
    id BIGINT NOT NULL COMMENT '主键ID',
    host VARCHAR(253) NOT NULL COMMENT '全局唯一的小写域名，不含端口和路径',
    scheme VARCHAR(8) NOT NULL DEFAULT 'https' COMMENT '公开链接协议，生产使用https',
    public_port INT NULL COMMENT '对外端口，空表示协议默认端口',
    tenant_id BIGINT NOT NULL COMMENT '所属租户，必须来自可信上下文',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    usable INT NOT NULL DEFAULT 1 COMMENT '是否启用，1启用0停用',
    operable INT NOT NULL DEFAULT 1 COMMENT '是否允许操作',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除，删除不释放唯一标识',
    create_id BIGINT NULL COMMENT '用户创建人，系统调用为空',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间，由应用更新',
    PRIMARY KEY (id),
    UNIQUE (host)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='短链域名';

CREATE TABLE IF NOT EXISTS shortlink_namespace (
    id BIGINT NOT NULL COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '所属短链配置ID',
    domain_id BIGINT NOT NULL COMMENT '所属短链域名ID',
    path_code VARCHAR(32) NOT NULL COMMENT '域名下的路径段，如a，不含斜杠',
    generate_type VARCHAR(16) NOT NULL COMMENT 'AUTO号段生成或CUSTOM调用方指定',
    code_length INT NOT NULL COMMENT '短码长度，只支持6或8',
    tenant_id BIGINT NOT NULL COMMENT '所属租户，必须来自可信上下文',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    usable INT NOT NULL DEFAULT 1 COMMENT '是否启用，1启用0停用',
    operable INT NOT NULL DEFAULT 1 COMMENT '是否允许操作',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除，删除不释放唯一标识',
    create_id BIGINT NULL COMMENT '用户创建人，系统调用为空',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间，由应用更新',
    PRIMARY KEY (id),
    UNIQUE (domain_id, path_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='短链路径配置';

CREATE TABLE IF NOT EXISTS shortlink_config_grant (
    id BIGINT NOT NULL COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '短链配置ID',
    credential_id BIGINT NOT NULL COMMENT '同租户系统调用凭证ID',
    tenant_id BIGINT NOT NULL COMMENT '所属租户，必须来自可信上下文',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    usable INT NOT NULL DEFAULT 1 COMMENT '是否启用，1启用0停用',
    operable INT NOT NULL DEFAULT 1 COMMENT '是否允许操作',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除，删除不释放唯一标识',
    create_id BIGINT NULL COMMENT '用户创建人，系统调用为空',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间，由应用更新',
    PRIMARY KEY (id),
    UNIQUE (tenant_id, config_id, credential_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='短链配置调用授权';

CREATE TABLE IF NOT EXISTS shortlink_link (
    id BIGINT NOT NULL COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '短链配置ID',
    namespace_id BIGINT NOT NULL COMMENT '路径配置ID',
    short_code VARCHAR(8) NOT NULL COMMENT '区分大小写的Base62短码',
    target_url VARCHAR(4096) NOT NULL COMMENT '生成时解析的目标地址快照',
    business_code VARCHAR(512) NULL COMMENT '调用方业务编码，不是配置编码',
    request_id VARCHAR(128) NOT NULL COMMENT '配置内唯一的创建幂等标识',
    request_hash VARCHAR(64) NOT NULL COMMENT '请求参数SHA256摘要，用于检查幂等冲突',
    expire_time DATETIME(3) NULL COMMENT '短链失效时间，空表示永久',
    credential_id BIGINT NULL COMMENT '系统调用创建时的凭证ID，与用户创建人分开',
    tenant_id BIGINT NOT NULL COMMENT '所属租户，必须来自可信上下文',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    usable INT NOT NULL DEFAULT 1 COMMENT '是否启用，1启用0停用',
    operable INT NOT NULL DEFAULT 1 COMMENT '是否允许操作',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除，删除不释放唯一标识',
    create_id BIGINT NULL COMMENT '用户创建人，系统调用为空',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间，由应用更新',
    PRIMARY KEY (id),
    UNIQUE (namespace_id, short_code),
    UNIQUE (tenant_id, config_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='短链记录';

-- 号段不得删除或回退；namespace删除后仍保留高水位。
CREATE TABLE IF NOT EXISTS shortlink_sequence (
    namespace_id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    max_allocated_id BIGINT NOT NULL DEFAULT 0,
    step INT NOT NULL DEFAULT 1000,
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='短链号段高水位';

-- 模板租户菜单。执行前必须停写并备份，确认infra应用和系统管理目录各恰好一条，且相关权限不存在。
-- 每条插入只读取临时表一次，兼容MySQL 5.7。部分执行失败时先核验已插入记录，不要直接重跑菜单段。
CREATE TEMPORARY TABLE migration_shortlink_ids AS
SELECT p.id AS system_id, a.id AS app_id, (SELECT COALESCE(MAX(id),0) FROM upm_permission) AS base_id
FROM upm_permission p JOIN upm_application a ON a.id=p.app_id AND a.tenant_id=1 AND a.code='infra' AND a.deleted=0
WHERE p.tenant_id=1 AND p.parent_id=0 AND p.menu_url='system' AND p.device='web' AND p.deleted=0;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+1,system_id,app_id,1,'调用凭证',1,'api-credential','system/api-credential/index','lucide:key-round',NULL,18,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+2,base_id+1,app_id,1,'列表',2,NULL,NULL,NULL,'system:api-credential:list',1,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+3,base_id+1,app_id,1,'新增',2,NULL,NULL,NULL,'system:api-credential:add',2,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+4,base_id+1,app_id,1,'编辑及授权',2,NULL,NULL,NULL,'system:api-credential:edit',3,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+5,0,app_id,1,'短链管理',0,'shortlink',NULL,'lucide:link',NULL,4,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+6,base_id+5,app_id,1,'短链配置',1,'config','shortlink/config/index','lucide:settings-2',NULL,2,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+7,base_id+6,app_id,1,'列表',2,NULL,NULL,NULL,'shortlink:config:list',1,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+8,base_id+6,app_id,1,'新增和编辑',2,NULL,NULL,NULL,'shortlink:config:edit',2,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+9,base_id+6,app_id,1,'调用授权',2,NULL,NULL,NULL,'shortlink:config:grant',3,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+10,base_id+5,app_id,1,'短链域名',1,'domain','shortlink/domain/index','lucide:globe',NULL,1,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+11,base_id+10,app_id,1,'列表',2,NULL,NULL,NULL,'shortlink:domain:list',1,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+12,base_id+10,app_id,1,'新增和编辑',2,NULL,NULL,NULL,'shortlink:domain:edit',2,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+13,base_id+5,app_id,1,'短链列表',1,'link','shortlink/link/index','lucide:link-2',NULL,3,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+14,base_id+13,app_id,1,'列表',2,NULL,NULL,NULL,'shortlink:link:list',1,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+15,base_id+13,app_id,1,'创建',2,NULL,NULL,NULL,'shortlink:link:add',2,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+16,base_id+13,app_id,1,'续期',2,NULL,NULL,NULL,'shortlink:link:renew',3,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+17,base_id+13,app_id,1,'停用',2,NULL,NULL,NULL,'shortlink:link:disable',4,'web',1,1,0,1 FROM migration_shortlink_ids;

INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+18,base_id+13,app_id,1,'启用',2,NULL,NULL,NULL,'shortlink:link:enable',5,'web',1,1,0,1 FROM migration_shortlink_ids;
INSERT INTO upm_permission
(id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,component,menu_icon,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT base_id+19,base_id+13,app_id,1,'删除',2,NULL,NULL,NULL,'shortlink:link:delete',6,'web',1,1,0,1 FROM migration_shortlink_ids;

INSERT INTO upm_role_permission(role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,1,'web' FROM upm_role r CROSS JOIN upm_permission p CROSS JOIN migration_shortlink_ids m
WHERE r.tenant_id=1 AND r.id IN (10,20) AND p.id BETWEEN m.base_id+1 AND m.base_id+19;
DROP TABLE migration_shortlink_ids;
