-- Notice 站内消息与邮件。执行前备份并停止写入；MySQL 5.7+/PostgreSQL。

-- IF NOT EXISTS支持完整执行后的重跑；已有同名表时需核验结构，不能用于修复部分字段差异。

-- 执行期间不要并发创建菜单；新菜单使用现有最大ID后的连续区间。

CREATE TABLE IF NOT EXISTS notice_message (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '所属租户',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    deleted SMALLINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_id BIGINT NULL COMMENT '创建用户',
    create_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    title VARCHAR(200) NULL COMMENT '标题',
    content MEDIUMTEXT NULL COMMENT '正文',
    content_type VARCHAR(20) NULL COMMENT 'TEXT、HTML、MARKDOWN',
    message_type VARCHAR(30) NULL COMMENT 'ANNOUNCEMENT、APPROVAL、BUSINESS',
    business_type VARCHAR(100) NULL COMMENT '来源业务类型',
    business_id VARCHAR(128) NULL COMMENT '来源业务ID或批次号',
    request_id VARCHAR(128) NULL COMMENT '可选幂等请求号',
    target_config MEDIUMTEXT NULL COMMENT '接收范围JSON',
    recipient_snapshot LONGTEXT NULL COMMENT '发布时冻结的接收人快照，不向列表输出',
    action_url VARCHAR(1000) NULL COMMENT '站内相对跳转路径',
    publish_status VARCHAR(30) NULL COMMENT '发布状态',
    scheduled_time DATETIME(3) NULL COMMENT '计划发布时间',
    publish_time DATETIME(3) NULL COMMENT '发布完成时间',
    recipient_count BIGINT NOT NULL DEFAULT 0 COMMENT '已生成接收记录数',
    failure_reason TEXT NULL COMMENT '发布失败原因',
    lease_token VARCHAR(64) NULL COMMENT '任务处理租约标识',
    lease_until DATETIME(3) NULL COMMENT '任务租约到期时间',
    request_hash VARCHAR(64) NULL COMMENT '幂等请求内容摘要',
    UNIQUE KEY uk_notice_message_request (tenant_id, request_id),
    KEY idx_notice_message_task (publish_status, lease_until, scheduled_time),
    KEY idx_notice_message_business (tenant_id, business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='站内消息';

CREATE TABLE IF NOT EXISTS notice_mail_config (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '所属租户',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    deleted SMALLINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_id BIGINT NULL COMMENT '创建用户',
    create_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    name VARCHAR(100) NULL COMMENT '配置名称',
    code VARCHAR(100) NOT NULL COMMENT '租户内有效配置编码',
    smtp_host VARCHAR(255) NULL COMMENT 'SMTP地址',
    smtp_port INT NULL COMMENT 'SMTP端口',
    security_mode VARCHAR(20) NULL COMMENT 'NONE、TLS、STARTTLS',
    username VARCHAR(255) NULL COMMENT 'SMTP用户名',
    password_cipher TEXT NULL COMMENT 'SMTP授权码密文，禁止回显',
    from_address VARCHAR(255) NULL COMMENT '发件邮箱',
    from_name VARCHAR(100) NULL COMMENT '发件显示名称',
    reply_to VARCHAR(255) NULL COMMENT '回复邮箱',
    usable SMALLINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(500) NULL COMMENT '说明',
    active_code VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted=0 THEN code ELSE NULL END) VIRTUAL,
    UNIQUE KEY uk_notice_mail_config_code (tenant_id,active_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='租户SMTP发件配置';

CREATE TABLE IF NOT EXISTS notice_mail_template (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '所属租户',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    deleted SMALLINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_id BIGINT NULL COMMENT '创建用户',
    create_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    name VARCHAR(100) NULL COMMENT '模板名称',
    code VARCHAR(100) NOT NULL COMMENT '租户内有效模板编码',
    subject VARCHAR(500) NULL COMMENT '主题模板',
    content_type VARCHAR(20) NULL COMMENT 'HTML、TEXT、MARKDOWN',
    content MEDIUMTEXT NULL COMMENT '模板源码',
    variable_config TEXT NULL COMMENT '变量定义JSON',
    usable SMALLINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(500) NULL COMMENT '说明',
    active_code VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN deleted=0 THEN code ELSE NULL END) VIRTUAL,
    UNIQUE KEY uk_notice_mail_template_code (tenant_id,active_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='租户邮件模板';

CREATE TABLE IF NOT EXISTS notice_mail (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '所属租户',
    version INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
    deleted SMALLINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_id BIGINT NULL COMMENT '创建用户',
    create_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    config_id BIGINT NULL COMMENT '发件配置ID',
    template_id BIGINT NULL COMMENT '可选来源模板ID',
    subject VARCHAR(500) NULL COMMENT '主题模板快照',
    content_type VARCHAR(20) NULL COMMENT '正文格式快照',
    content MEDIUMTEXT NULL COMMENT '正文模板快照',
    variable_config TEXT NULL COMMENT '变量定义快照',
    common_params MEDIUMTEXT NULL COMMENT '公共参数JSON',
    from_address VARCHAR(255) NULL COMMENT '发件地址快照',
    from_name VARCHAR(100) NULL COMMENT '发件名称快照',
    reply_to VARCHAR(255) NULL COMMENT '回复地址快照',
    business_type VARCHAR(100) NULL COMMENT '来源业务类型',
    business_id VARCHAR(128) NULL COMMENT '来源业务ID或批次号',
    request_id VARCHAR(128) NULL COMMENT '可选幂等请求号',
    send_status VARCHAR(30) NULL COMMENT '发送状态',
    scheduled_time DATETIME(3) NULL COMMENT '计划发送时间',
    start_time DATETIME(3) NULL COMMENT '首次开始发送时间',
    finish_time DATETIME(3) NULL COMMENT '本轮处理完成时间',
    recipient_count BIGINT NOT NULL DEFAULT 0 COMMENT '收件人数',
    success_count BIGINT NOT NULL DEFAULT 0 COMMENT 'SMTP接受人数',
    failure_count BIGINT NOT NULL DEFAULT 0 COMMENT '最终失败人数',
    failure_reason TEXT NULL COMMENT '任务整体失败原因',
    lease_token VARCHAR(64) NULL COMMENT '处理租约标识',
    lease_until DATETIME(3) NULL COMMENT '处理租约到期时间',
    request_hash VARCHAR(64) NULL COMMENT '幂等请求内容摘要',
    UNIQUE KEY uk_notice_mail_request (tenant_id, request_id),
    KEY idx_notice_mail_task (send_status, lease_until, scheduled_time),
    KEY idx_notice_mail_business (tenant_id, business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='邮件发送任务';

CREATE TABLE IF NOT EXISTS notice_message_recipient (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '所属租户',
    message_id BIGINT NOT NULL COMMENT '站内消息ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    recipient_name VARCHAR(100) NULL COMMENT '接收姓名快照',
    read_time DATETIME(3) NULL COMMENT '首次阅读时间',
    removed_time DATETIME(3) NULL COMMENT '从个人列表移除时间',
    create_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_notice_message_recipient (tenant_id, message_id, user_id),
    KEY idx_notice_message_user (tenant_id, user_id, create_time, id),
    KEY idx_notice_message_unread (tenant_id, user_id, read_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='站内消息接收记录';

CREATE TABLE IF NOT EXISTS notice_mail_recipient (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '所属租户',
    mail_id BIGINT NOT NULL COMMENT '邮件任务ID',
    user_id BIGINT NULL COMMENT '可选系统用户ID',
    email VARCHAR(255) NOT NULL COMMENT '接收邮箱',
    recipient_name VARCHAR(100) NULL COMMENT '收件人名称',
    params MEDIUMTEXT NULL COMMENT '个人变量JSON快照',
    send_status VARCHAR(30) NULL COMMENT '接收记录发送状态',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '累计真实尝试次数，人工重试不清零',
    next_retry_time DATETIME(3) NULL COMMENT '下次尝试时间',
    last_attempt_time DATETIME(3) NULL COMMENT '最近尝试开始时间',
    sent_time DATETIME(3) NULL COMMENT 'SMTP接受成功时间',
    message_id VARCHAR(255) NULL COMMENT 'SMTP Message-ID',
    failure_reason TEXT NULL COMMENT '最近失败原因',
    retry_limit INT NOT NULL DEFAULT 3 COMMENT '本轮累计尝试上限，人工重试追加预算',
    create_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_notice_mail_recipient (tenant_id, mail_id, email),
    KEY idx_notice_mail_delivery (tenant_id, mail_id, send_status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='邮件收件及发送结果';


-- 为已存在租户的infra应用添加菜单；新租户继续复用现有模板复制机制。

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),0,a.id,a.tenant_id,'通知中心',0,'notice','lucide:bell','','',20,'web',1,1,0,1
FROM upm_application a WHERE a.code='infra' AND a.deleted=0 AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.menu_url='notice');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'通知管理',1,'manage','lucide:send','notice/manage/index',NULL,1,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='notice' WHERE a.code='infra' AND a.deleted=0 AND p.permission_type=0 AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.menu_url='manage');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'邮件配置',1,'config','ant-design:setting-outlined','notice/config/index','notice:mail-config:list',2,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='notice' WHERE a.code='infra' AND a.deleted=0 AND p.permission_type=0 AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-config:list');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'邮件模板',1,'template','lucide:panels-top-left','notice/template/index','notice:mail-template:list',3,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='notice' WHERE a.code='infra' AND a.deleted=0 AND p.permission_type=0 AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-template:list');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'站内列表',2,'','','','notice:message:list',1,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:message:list');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'编辑站内消息',2,'','','','notice:message:edit',2,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:message:edit');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'发布站内消息',2,'','','','notice:message:publish',3,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:message:publish');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'删除站内消息',2,'','','','notice:message:delete',4,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:message:delete');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'邮件列表',2,'','','','notice:mail:list',5,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail:list');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'编辑邮件',2,'','','','notice:mail:edit',6,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail:edit');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'发送重试邮件',2,'','','','notice:mail:send',7,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail:send');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'删除邮件',2,'','','','notice:mail:delete',8,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail:delete');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'编辑邮件配置',2,'','','','notice:mail-config:edit',1,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='config' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/config/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-config:edit');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'删除邮件配置',2,'','','','notice:mail-config:delete',2,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='config' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/config/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-config:delete');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'编辑模板',2,'','','','notice:mail-template:edit',1,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='template' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/template/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-template:edit');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'删除模板',2,'','','','notice:mail-template:delete',2,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='template' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/template/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-template:delete');

SET @notice_next=(SELECT COALESCE(MAX(id),0) FROM upm_permission);

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (@notice_next:=@notice_next+1),p.id,a.id,a.tenant_id,'测试邮件',2,'','','','notice:mail-template:test',3,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='template' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/template/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-template:test');

UPDATE upm_permission p JOIN upm_application a ON a.id=p.app_id
JOIN upm_application base_app ON base_app.tenant_id=1 AND base_app.code='infra' AND base_app.deleted=0
JOIN upm_permission base ON base.app_id=base_app.id AND base.tenant_id=1 AND base.device='web' AND base.deleted=0
AND ((p.permission_type=2 AND p.permission_code=base.permission_code) OR (p.permission_type<>2 AND p.menu_url=base.menu_url AND p.component=base.component AND base.permission_type=p.permission_type))
SET p.source_id=base.id WHERE a.code='infra' AND p.tenant_id<>1 AND p.source_id IS NULL AND p.deleted=0
AND ((p.menu_url='notice' AND p.permission_type=0) OR p.component IN ('notice/manage/index','notice/config/index','notice/template/index') OR p.permission_code LIKE 'notice:%');

INSERT INTO upm_role_permission (role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,r.tenant_id,'web' FROM upm_role r JOIN upm_permission p ON p.tenant_id=r.tenant_id
JOIN upm_application a ON a.id=p.app_id AND a.code='infra' AND a.deleted=0
WHERE r.deleted=0 AND r.role_code IN ('platformAdmin','tenantAdmin') AND p.deleted=0 AND p.device='web'
AND (p.permission_code LIKE 'notice:%' OR p.component IN ('notice/manage/index','notice/config/index','notice/template/index') OR p.menu_url='notice')
AND NOT EXISTS (SELECT 1 FROM upm_role_permission x WHERE x.role_id=r.id AND x.permission_id=p.id AND x.tenant_id=r.tenant_id);
-- 核验：6张notice表；infra应用通知中心、通知管理、邮件配置、邮件模板；管理员重新登录刷新权限。
