-- Notice 站内消息与邮件。执行前备份并停止写入；MySQL 5.7+/PostgreSQL。

-- IF NOT EXISTS支持完整执行后的重跑；已有同名表时需核验结构，不能用于修复部分字段差异。

-- 执行期间不要并发创建菜单；新菜单使用现有最大ID后的连续区间。

CREATE TABLE IF NOT EXISTS notice_message (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_id BIGINT NULL,
    create_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    title VARCHAR(200) NULL,
    content TEXT NULL,
    content_type VARCHAR(20) NULL,
    message_type VARCHAR(30) NULL,
    business_type VARCHAR(100) NULL,
    business_id VARCHAR(128) NULL,
    request_id VARCHAR(128) NULL,
    target_config TEXT NULL,
    recipient_snapshot TEXT NULL,
    action_url VARCHAR(1000) NULL,
    publish_status VARCHAR(30) NULL,
    scheduled_time TIMESTAMP(3) NULL,
    publish_time TIMESTAMP(3) NULL,
    recipient_count BIGINT NOT NULL DEFAULT 0,
    failure_reason TEXT NULL,
    lease_token VARCHAR(64) NULL,
    lease_until TIMESTAMP(3) NULL,
    request_hash VARCHAR(64) NULL
);

COMMENT ON TABLE notice_message IS '站内消息';

COMMENT ON COLUMN notice_message.id IS '主键ID';

COMMENT ON COLUMN notice_message.tenant_id IS '所属租户';

COMMENT ON COLUMN notice_message.version IS '乐观锁版本';

COMMENT ON COLUMN notice_message.deleted IS '逻辑删除';

COMMENT ON COLUMN notice_message.create_id IS '创建用户';

COMMENT ON COLUMN notice_message.create_time IS '创建时间';

COMMENT ON COLUMN notice_message.update_time IS '更新时间';

COMMENT ON COLUMN notice_message.title IS '标题';

COMMENT ON COLUMN notice_message.content IS '正文';

COMMENT ON COLUMN notice_message.content_type IS 'TEXT、HTML、MARKDOWN';

COMMENT ON COLUMN notice_message.message_type IS 'ANNOUNCEMENT、APPROVAL、BUSINESS';

COMMENT ON COLUMN notice_message.business_type IS '来源业务类型';

COMMENT ON COLUMN notice_message.business_id IS '来源业务ID或批次号';

COMMENT ON COLUMN notice_message.request_id IS '可选幂等请求号';

COMMENT ON COLUMN notice_message.target_config IS '接收范围JSON';

COMMENT ON COLUMN notice_message.recipient_snapshot IS '发布时冻结的接收人快照，不向列表输出';

COMMENT ON COLUMN notice_message.action_url IS '站内相对跳转路径';

COMMENT ON COLUMN notice_message.publish_status IS '发布状态';

COMMENT ON COLUMN notice_message.scheduled_time IS '计划发布时间';

COMMENT ON COLUMN notice_message.publish_time IS '发布完成时间';

COMMENT ON COLUMN notice_message.recipient_count IS '已生成接收记录数';

COMMENT ON COLUMN notice_message.failure_reason IS '发布失败原因';

COMMENT ON COLUMN notice_message.lease_token IS '任务处理租约标识';

COMMENT ON COLUMN notice_message.lease_until IS '任务租约到期时间';

COMMENT ON COLUMN notice_message.request_hash IS '幂等请求内容摘要';

CREATE UNIQUE INDEX IF NOT EXISTS uk_notice_message_request ON notice_message (tenant_id, request_id);

CREATE INDEX IF NOT EXISTS idx_notice_message_task ON notice_message (publish_status, lease_until, scheduled_time);

CREATE INDEX IF NOT EXISTS idx_notice_message_business ON notice_message (tenant_id, business_type, business_id);

CREATE TABLE IF NOT EXISTS notice_mail_config (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_id BIGINT NULL,
    create_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    name VARCHAR(100) NULL,
    code VARCHAR(100) NOT NULL,
    smtp_host VARCHAR(255) NULL,
    smtp_port INT NULL,
    security_mode VARCHAR(20) NULL,
    username VARCHAR(255) NULL,
    password_cipher TEXT NULL,
    from_address VARCHAR(255) NULL,
    from_name VARCHAR(100) NULL,
    reply_to VARCHAR(255) NULL,
    usable SMALLINT NOT NULL DEFAULT 1,
    description VARCHAR(500) NULL
);

COMMENT ON TABLE notice_mail_config IS '租户SMTP发件配置';

COMMENT ON COLUMN notice_mail_config.id IS '主键ID';

COMMENT ON COLUMN notice_mail_config.tenant_id IS '所属租户';

COMMENT ON COLUMN notice_mail_config.version IS '乐观锁版本';

COMMENT ON COLUMN notice_mail_config.deleted IS '逻辑删除';

COMMENT ON COLUMN notice_mail_config.create_id IS '创建用户';

COMMENT ON COLUMN notice_mail_config.create_time IS '创建时间';

COMMENT ON COLUMN notice_mail_config.update_time IS '更新时间';

COMMENT ON COLUMN notice_mail_config.name IS '配置名称';

COMMENT ON COLUMN notice_mail_config.code IS '租户内有效配置编码';

COMMENT ON COLUMN notice_mail_config.smtp_host IS 'SMTP地址';

COMMENT ON COLUMN notice_mail_config.smtp_port IS 'SMTP端口';

COMMENT ON COLUMN notice_mail_config.security_mode IS 'NONE、TLS、STARTTLS';

COMMENT ON COLUMN notice_mail_config.username IS 'SMTP用户名';

COMMENT ON COLUMN notice_mail_config.password_cipher IS 'SMTP授权码密文，禁止回显';

COMMENT ON COLUMN notice_mail_config.from_address IS '发件邮箱';

COMMENT ON COLUMN notice_mail_config.from_name IS '发件显示名称';

COMMENT ON COLUMN notice_mail_config.reply_to IS '回复邮箱';

COMMENT ON COLUMN notice_mail_config.usable IS '是否启用';

COMMENT ON COLUMN notice_mail_config.description IS '说明';

CREATE UNIQUE INDEX IF NOT EXISTS uk_notice_mail_config_code ON notice_mail_config (tenant_id,code) WHERE deleted=0;

CREATE TABLE IF NOT EXISTS notice_mail_template (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_id BIGINT NULL,
    create_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    name VARCHAR(100) NULL,
    code VARCHAR(100) NOT NULL,
    subject VARCHAR(500) NULL,
    content_type VARCHAR(20) NULL,
    content TEXT NULL,
    variable_config TEXT NULL,
    usable SMALLINT NOT NULL DEFAULT 1,
    description VARCHAR(500) NULL
);

COMMENT ON TABLE notice_mail_template IS '租户邮件模板';

COMMENT ON COLUMN notice_mail_template.id IS '主键ID';

COMMENT ON COLUMN notice_mail_template.tenant_id IS '所属租户';

COMMENT ON COLUMN notice_mail_template.version IS '乐观锁版本';

COMMENT ON COLUMN notice_mail_template.deleted IS '逻辑删除';

COMMENT ON COLUMN notice_mail_template.create_id IS '创建用户';

COMMENT ON COLUMN notice_mail_template.create_time IS '创建时间';

COMMENT ON COLUMN notice_mail_template.update_time IS '更新时间';

COMMENT ON COLUMN notice_mail_template.name IS '模板名称';

COMMENT ON COLUMN notice_mail_template.code IS '租户内有效模板编码';

COMMENT ON COLUMN notice_mail_template.subject IS '主题模板';

COMMENT ON COLUMN notice_mail_template.content_type IS 'HTML、TEXT、MARKDOWN';

COMMENT ON COLUMN notice_mail_template.content IS '模板源码';

COMMENT ON COLUMN notice_mail_template.variable_config IS '变量定义JSON';

COMMENT ON COLUMN notice_mail_template.usable IS '是否启用';

COMMENT ON COLUMN notice_mail_template.description IS '说明';

CREATE UNIQUE INDEX IF NOT EXISTS uk_notice_mail_template_code ON notice_mail_template (tenant_id,code) WHERE deleted=0;

CREATE TABLE IF NOT EXISTS notice_mail (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_id BIGINT NULL,
    create_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    config_id BIGINT NULL,
    template_id BIGINT NULL,
    subject VARCHAR(500) NULL,
    content_type VARCHAR(20) NULL,
    content TEXT NULL,
    variable_config TEXT NULL,
    common_params TEXT NULL,
    from_address VARCHAR(255) NULL,
    from_name VARCHAR(100) NULL,
    reply_to VARCHAR(255) NULL,
    business_type VARCHAR(100) NULL,
    business_id VARCHAR(128) NULL,
    request_id VARCHAR(128) NULL,
    send_status VARCHAR(30) NULL,
    scheduled_time TIMESTAMP(3) NULL,
    start_time TIMESTAMP(3) NULL,
    finish_time TIMESTAMP(3) NULL,
    recipient_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    failure_reason TEXT NULL,
    lease_token VARCHAR(64) NULL,
    lease_until TIMESTAMP(3) NULL,
    request_hash VARCHAR(64) NULL
);

COMMENT ON TABLE notice_mail IS '邮件发送任务';

COMMENT ON COLUMN notice_mail.id IS '主键ID';

COMMENT ON COLUMN notice_mail.tenant_id IS '所属租户';

COMMENT ON COLUMN notice_mail.version IS '乐观锁版本';

COMMENT ON COLUMN notice_mail.deleted IS '逻辑删除';

COMMENT ON COLUMN notice_mail.create_id IS '创建用户';

COMMENT ON COLUMN notice_mail.create_time IS '创建时间';

COMMENT ON COLUMN notice_mail.update_time IS '更新时间';

COMMENT ON COLUMN notice_mail.config_id IS '发件配置ID';

COMMENT ON COLUMN notice_mail.template_id IS '可选来源模板ID';

COMMENT ON COLUMN notice_mail.subject IS '主题模板快照';

COMMENT ON COLUMN notice_mail.content_type IS '正文格式快照';

COMMENT ON COLUMN notice_mail.content IS '正文模板快照';

COMMENT ON COLUMN notice_mail.variable_config IS '变量定义快照';

COMMENT ON COLUMN notice_mail.common_params IS '公共参数JSON';

COMMENT ON COLUMN notice_mail.from_address IS '发件地址快照';

COMMENT ON COLUMN notice_mail.from_name IS '发件名称快照';

COMMENT ON COLUMN notice_mail.reply_to IS '回复地址快照';

COMMENT ON COLUMN notice_mail.business_type IS '来源业务类型';

COMMENT ON COLUMN notice_mail.business_id IS '来源业务ID或批次号';

COMMENT ON COLUMN notice_mail.request_id IS '可选幂等请求号';

COMMENT ON COLUMN notice_mail.send_status IS '发送状态';

COMMENT ON COLUMN notice_mail.scheduled_time IS '计划发送时间';

COMMENT ON COLUMN notice_mail.start_time IS '首次开始发送时间';

COMMENT ON COLUMN notice_mail.finish_time IS '本轮处理完成时间';

COMMENT ON COLUMN notice_mail.recipient_count IS '收件人数';

COMMENT ON COLUMN notice_mail.success_count IS 'SMTP接受人数';

COMMENT ON COLUMN notice_mail.failure_count IS '最终失败人数';

COMMENT ON COLUMN notice_mail.failure_reason IS '任务整体失败原因';

COMMENT ON COLUMN notice_mail.lease_token IS '处理租约标识';

COMMENT ON COLUMN notice_mail.lease_until IS '处理租约到期时间';

COMMENT ON COLUMN notice_mail.request_hash IS '幂等请求内容摘要';

CREATE UNIQUE INDEX IF NOT EXISTS uk_notice_mail_request ON notice_mail (tenant_id, request_id);

CREATE INDEX IF NOT EXISTS idx_notice_mail_task ON notice_mail (send_status, lease_until, scheduled_time);

CREATE INDEX IF NOT EXISTS idx_notice_mail_business ON notice_mail (tenant_id, business_type, business_id);

CREATE TABLE IF NOT EXISTS notice_message_recipient (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NULL,
    read_time TIMESTAMP(3) NULL,
    removed_time TIMESTAMP(3) NULL,
    create_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3)
);

COMMENT ON TABLE notice_message_recipient IS '站内消息接收记录';

COMMENT ON COLUMN notice_message_recipient.id IS '主键ID';

COMMENT ON COLUMN notice_message_recipient.tenant_id IS '所属租户';

COMMENT ON COLUMN notice_message_recipient.message_id IS '站内消息ID';

COMMENT ON COLUMN notice_message_recipient.user_id IS '接收用户ID';

COMMENT ON COLUMN notice_message_recipient.recipient_name IS '接收姓名快照';

COMMENT ON COLUMN notice_message_recipient.read_time IS '首次阅读时间';

COMMENT ON COLUMN notice_message_recipient.removed_time IS '从个人列表移除时间';

COMMENT ON COLUMN notice_message_recipient.create_time IS '创建时间';

COMMENT ON COLUMN notice_message_recipient.update_time IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_notice_message_recipient ON notice_message_recipient (tenant_id, message_id, user_id);

CREATE INDEX IF NOT EXISTS idx_notice_message_user ON notice_message_recipient (tenant_id, user_id, create_time, id);

CREATE INDEX IF NOT EXISTS idx_notice_message_unread ON notice_message_recipient (tenant_id, user_id, read_time);

CREATE TABLE IF NOT EXISTS notice_mail_recipient (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    mail_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(100) NULL,
    params TEXT NULL,
    send_status VARCHAR(30) NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_time TIMESTAMP(3) NULL,
    last_attempt_time TIMESTAMP(3) NULL,
    sent_time TIMESTAMP(3) NULL,
    message_id VARCHAR(255) NULL,
    failure_reason TEXT NULL,
    retry_limit INT NOT NULL DEFAULT 3,
    create_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3)
);

COMMENT ON TABLE notice_mail_recipient IS '邮件收件及发送结果';

COMMENT ON COLUMN notice_mail_recipient.id IS '主键ID';

COMMENT ON COLUMN notice_mail_recipient.tenant_id IS '所属租户';

COMMENT ON COLUMN notice_mail_recipient.mail_id IS '邮件任务ID';

COMMENT ON COLUMN notice_mail_recipient.user_id IS '可选系统用户ID';

COMMENT ON COLUMN notice_mail_recipient.email IS '接收邮箱';

COMMENT ON COLUMN notice_mail_recipient.recipient_name IS '收件人名称';

COMMENT ON COLUMN notice_mail_recipient.params IS '个人变量JSON快照';

COMMENT ON COLUMN notice_mail_recipient.send_status IS '接收记录发送状态';

COMMENT ON COLUMN notice_mail_recipient.attempt_count IS '累计真实尝试次数，人工重试不清零';

COMMENT ON COLUMN notice_mail_recipient.next_retry_time IS '下次尝试时间';

COMMENT ON COLUMN notice_mail_recipient.last_attempt_time IS '最近尝试开始时间';

COMMENT ON COLUMN notice_mail_recipient.sent_time IS 'SMTP接受成功时间';

COMMENT ON COLUMN notice_mail_recipient.message_id IS 'SMTP Message-ID';

COMMENT ON COLUMN notice_mail_recipient.failure_reason IS '最近失败原因';

COMMENT ON COLUMN notice_mail_recipient.retry_limit IS '本轮累计尝试上限，人工重试追加预算';

COMMENT ON COLUMN notice_mail_recipient.create_time IS '创建时间';

COMMENT ON COLUMN notice_mail_recipient.update_time IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_notice_mail_recipient ON notice_mail_recipient (tenant_id, mail_id, email);

CREATE INDEX IF NOT EXISTS idx_notice_mail_delivery ON notice_mail_recipient (tenant_id, mail_id, send_status, next_retry_time);


-- 为已存在租户的infra应用添加菜单；新租户继续复用现有模板复制机制。

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),0,a.id,a.tenant_id,'通知中心',0,'notice','lucide:bell','','',20,'web',1,1,0,1
FROM upm_application a WHERE a.code='infra' AND a.deleted=0 AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.menu_url='notice');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'通知管理',1,'manage','lucide:send','notice/manage/index',NULL,1,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='notice' WHERE a.code='infra' AND a.deleted=0 AND p.permission_type=0 AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.menu_url='manage');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'邮件配置',1,'config','ant-design:setting-outlined','notice/config/index','notice:mail-config:list',2,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='notice' WHERE a.code='infra' AND a.deleted=0 AND p.permission_type=0 AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-config:list');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'邮件模板',1,'template','lucide:panels-top-left','notice/template/index','notice:mail-template:list',3,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='notice' WHERE a.code='infra' AND a.deleted=0 AND p.permission_type=0 AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-template:list');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'站内列表',2,'','','','notice:message:list',1,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:message:list');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'编辑站内消息',2,'','','','notice:message:edit',2,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:message:edit');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'发布站内消息',2,'','','','notice:message:publish',3,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:message:publish');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'删除站内消息',2,'','','','notice:message:delete',4,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:message:delete');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'邮件列表',2,'','','','notice:mail:list',5,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail:list');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'编辑邮件',2,'','','','notice:mail:edit',6,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail:edit');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'发送重试邮件',2,'','','','notice:mail:send',7,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail:send');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'删除邮件',2,'','','','notice:mail:delete',8,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='manage' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/manage/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail:delete');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'编辑邮件配置',2,'','','','notice:mail-config:edit',1,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='config' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/config/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-config:edit');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'删除邮件配置',2,'','','','notice:mail-config:delete',2,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='config' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/config/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-config:delete');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'编辑模板',2,'','','','notice:mail-template:edit',1,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='template' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/template/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-template:edit');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'删除模板',2,'','','','notice:mail-template:delete',2,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='template' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/template/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-template:delete');

INSERT INTO upm_permission (id,parent_id,app_id,tenant_id,permission_name,permission_type,menu_url,menu_icon,component,permission_code,sort_number,device,operable,usable,deleted,version)
SELECT (SELECT COALESCE(MAX(id),0) FROM upm_permission)+ROW_NUMBER() OVER (ORDER BY a.tenant_id,a.id),p.id,a.id,a.tenant_id,'测试邮件',2,'','','','notice:mail-template:test',3,'web',1,1,0,1
FROM upm_application a JOIN upm_permission p ON p.app_id=a.id AND p.tenant_id=a.tenant_id AND p.device='web' AND p.deleted=0 AND p.menu_url='template' WHERE a.code='infra' AND a.deleted=0 AND p.component='notice/template/index' AND NOT EXISTS (SELECT 1 FROM upm_permission x WHERE x.app_id=a.id AND x.tenant_id=a.tenant_id AND x.device='web' AND x.deleted=0 AND x.permission_code='notice:mail-template:test');

UPDATE upm_permission p SET source_id=base.id FROM upm_application a,upm_application base_app,upm_permission base
WHERE p.app_id=a.id AND a.code='infra' AND p.tenant_id<>1 AND p.source_id IS NULL AND p.deleted=0
AND base_app.tenant_id=1 AND base_app.code='infra' AND base_app.deleted=0 AND base.app_id=base_app.id
AND base.tenant_id=1 AND base.device='web' AND base.deleted=0
AND ((p.permission_type=2 AND p.permission_code=base.permission_code) OR (p.permission_type<>2 AND p.menu_url=base.menu_url AND p.component=base.component AND base.permission_type=p.permission_type))
AND ((p.menu_url='notice' AND p.permission_type=0) OR p.component IN ('notice/manage/index','notice/config/index','notice/template/index') OR p.permission_code LIKE 'notice:%');

INSERT INTO upm_role_permission (role_id,permission_id,tenant_id,device)
SELECT r.id,p.id,r.tenant_id,'web' FROM upm_role r JOIN upm_permission p ON p.tenant_id=r.tenant_id
JOIN upm_application a ON a.id=p.app_id AND a.code='infra' AND a.deleted=0
WHERE r.deleted=0 AND r.role_code IN ('platformAdmin','tenantAdmin') AND p.deleted=0 AND p.device='web'
AND (p.permission_code LIKE 'notice:%' OR p.component IN ('notice/manage/index','notice/config/index','notice/template/index') OR p.menu_url='notice')
AND NOT EXISTS (SELECT 1 FROM upm_role_permission x WHERE x.role_id=r.id AND x.permission_id=p.id AND x.tenant_id=r.tenant_id);
-- 核验：6张notice表；infra应用通知中心、通知管理、邮件配置、邮件模板；管理员重新登录刷新权限。
