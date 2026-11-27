# Notice 站内消息与邮件

## 范围与依赖方向

本轮实现站内消息、租户 SMTP 发件配置、HTML/纯文本/Markdown 邮件模板、邮件异步发送与重试。短信、钉钉、飞书预留在 notice 模块内，尚未实现，不引入 sms4j。

- `notice-api`：请求响应、实体、枚举、`MessageService`、`MailService` 和 `NoticeDataProvider`。
- `notice-core`：接收范围展开、冻结快照、消息发布、个人收件箱、模板渲染、SMTP 与持久化任务处理。
- `infra-start`：`UpmNoticeDataProvider` 仅查询用户、部门、角色等基本数据，不组装通知。
- 不依赖 `upm-core`、`flow-core`。单独部署时实现 Provider，提供可信租户/用户上下文以及宿主 MyBatis 租户、分页、乐观锁拦截器。主表和接收表的租户字段均为 BIGINT。
- Flow、短链等业务不会因为引入 notice 自动发送消息；业务在自身事务中调用 API Service。以后微服务可替换数据 Provider，并在独立 HTTP/RPC 边界接入服务认证，不复用不可信客户端 tenantId。

## 首次安装

已有库备份、停止写入后，执行以下一种增量脚本：

- [MySQL 5.7+](../../sql/migration/20261127_notice.mysql.sql)
- [PostgreSQL](../../sql/migration/20261127_notice.postgres.sql)

新库只执行对应 `sql/full/` 全量脚本，不叠加执行增量。脚本增加六张表和各租户 infra 应用下的通知菜单，并为现有内置管理员关联按钮。菜单ID按当前最大ID递增，迁移期间禁止并发写入。完整执行后可重跑；IF NOT EXISTS不能修复部分建表后的字段差异，部分失败应核验后补齐。

新增租户继续使用现有应用菜单复制机制，不复制 SMTP 凭据、模板、历史消息或收件记录。其他已有租户的通知菜单记录关联租户1模板来源，避免后续模板同步重复新增。

在部署环境或外部配置中设置：

```properties
# 执行SQL后启用本地扫描，默认关闭。
notice.task.enabled=true
notice.task.interval-millis=5000
notice.batch-size=500
notice.max-recipients=100000
notice.max-attempts=3
notice.retry-delays-seconds=60,300
notice.lease-seconds=120
notice.smtp-timeout-millis=10000
# 仅使用站内消息不需要此值；发送邮件前必须配置。不要提交真实密钥。
notice.secret-key=${NOTICE_SECRET_KEY:}
```

`infra-start` 也支持环境变量 `NOTICE_TASK_ENABLED=true`。主密钥使用 `openssl rand -base64 32` 生成；所有节点使用相同密钥，并与数据库备份一起妥善管理。更换密钥前必须迁移已保存的 SMTP 密文，不能直接替换后继续使用旧密文。

立即发布的站内通知、立即发送的邮件和手动邮件重试，在事务提交后异步触发。定时发布、邮件自动重试，以及服务重启或并发槽满后留下的待执行任务，由本地或外部调度补捞。建议有通知业务的环境保持至少一种调度方式。

宿主须启用 Spring 定时调度；infra-start 已启用。以后使用 PowerJob 等调度时关闭本地开关，由受信任任务调用 `NoticeTaskDispatcher.dispatch()`，建议5秒一次。不能把该跨租户扫描方法暴露为匿名HTTP接口。

## 表结构

| 表 | 用途 |
|---|---|
| notice_message | 站内正文、分类、接收范围、冻结快照、发布状态、计划时间、租约 |
| notice_message_recipient | 每个接收用户一条，姓名快照、首次阅读时间、个人移除时间 |
| notice_mail_config | 租户 SMTP 配置，授权码 AES-256-GCM 加密且不回显 |
| notice_mail_template | 租户模板编码、主题、正文格式/源码、变量定义 |
| notice_mail | 一次邮件发送的正文和发件信息快照、变量、计划时间、处理状态 |
| notice_mail_recipient | 每个邮箱一条，个人变量、发送结果、累计尝试次数、下次重试时间 |

主表包含 id、tenant_id、version、deleted、create_id、create_time、update_time，不混用 usable/operable 与发布状态；只有配置和模板存在 usable。接收表不使用逻辑删除和乐观锁。个人移除消息使用 removed_time，保留接收事实和统计。

SMTP配置/模板的编码在同租户未删除记录内唯一，MySQL使用生成列唯一索引，PostgreSQL使用部分唯一索引；删除后可以重新创建同编码。消息/邮件 request_id 在同租户对应渠道内唯一，不因逻辑删除释放。数据库自增或默认账号不参与 ID 分配，统一使用项目雪花ID。

## 发布和接收规则

分类枚举为 ANNOUNCEMENT（系统公告）、APPROVAL（审批通知）、BUSINESS（业务提醒），与站内/邮件渠道独立。

后台任务会再次通过Provider检查租户存在且启用，租户停用/删除后不继续发送。

接收范围支持全员，或者用户、部门、角色取并集，部门可包含下级。空范围不能隐式发送全员。Provider只能返回本租户有效数据。站内消息在**提交发布时**解析并保存完整接收人ID和姓名快照；计划发布也是提交时冻结，后来新增用户不自动接收。多个分组查询不承诺跨远程系统的原子组织版本快照；如要求严格的同一时点组织快照，宿主Provider需提供相同版本的数据。

站内状态：DRAFT → SCHEDULED → PUBLISHING → PUBLISHED，异常为 FAILED。界面保存先生成草稿，再点击发布；选择未来计划时间时等待到期。站内发布和邮件网络调用使用独立并发预算，避免SMTP超时阻塞公告；后台500条一批写入，只有主表PUBLISHED的消息对接收人可见。接收唯一约束防重复，失败重试沿用原快照，重启或租约过期后恢复未完成任务。

发布人数为实际生成的接收记录数；已读以首次 read_time 为准，未读=发布人数−已读人数。发布中显示的接收人数是生成进度，未读统计以发布成功为准。姓名快照不因用户改名/删除丢失。已发布消息不能编辑或删除；更正时新建通知。

个人消息中心无需后台管理菜单权限，但所有操作必须登录，并限定到当前租户、本人的接收记录。其他用户ID或消息ID不能绕过接收关系。移除个人消息不改变管理员的发布/阅读统计。业务跳转只支持单个 `/` 开头的站内路由；目标业务仍执行自身授权检查。

### 站内正文编辑

新建站内通知默认使用富文本（HTML），通过本地打包的Tiptap编辑器直接设置标题、字号、粗体、颜色、对齐、列表、引用、链接和表格；可插入公开HTTP(S)图片地址并查看生成的HTML。当前没有接入文件上传。

也可选择纯文本或 Markdown。Markdown 保存源码，管理详情和个人收件箱使用与邮件共用的 CommonMark 渲染器显示。切换格式时转换正文：纯文本转HTML会转义标签，HTML转纯文本保留段落和换行并去掉排版。编辑器关闭时销毁实例，重新打开按保存内容回显。

详情和邮件预览在隔离iframe中加载共用排版样式，保留模板完整head/body和内联样式。禁止脚本、同源权限和表单提交；允许用户主动点击链接打开新标签页。正文格式与数据库字段不变，此项不需要额外迁移SQL。

## 邮件与模板

每个租户维护自己的 SMTP 和模板，不关联 appId。配置支持 TLS（隐式SSL）、STARTTLS（必须协商成功）、NONE；优先选择邮件供应商要求的安全方式和授权码。修改SMTP密码为空表示保持原值，接口响应不返回密文或明文。

模板格式为 HTML、TEXT、MARKDOWN，统一使用 content_type + content。HTML支持完整源码；Markdown先转换HTML，邮箱收到渲染结果。模板不绑定发件账号。模板页面提供内置HTML通知示例、变量编辑、隔离预览和真实测试邮件。

变量语法为 `${userName}`，仅允许已声明的字母/数字/下划线名称。必填参数缺失、未声明变量、表达式写法均拒绝，不执行SpEL。HTML变量转义，Markdown变量作为文本转义。主任务保存模板定义、正文、公共参数、发件地址/名称快照；接收记录保存个人参数，个人参数覆盖公共参数。模板修改不改变已保存发送任务。

邮件支持手动编写、选模板、外部邮箱，以及从当前租户用户/部门/角色选收件人；选中的用户必须有有效邮箱。发送前同邮箱去重，重复邮箱具有不同个人参数时拒绝。每个邮箱单独发送，不暴露其他接收地址。本轮不包含附件、抄送/密送、送达回执、阅读追踪。

邮件任务：DRAFT → SCHEDULED → SENDING → SUCCESS / PARTIAL_FAILED / FAILED。SMTP接受不代表最终投递或阅读，邮件不计算“已读人数”。

重试字段由发送程序维护：

- attempt_count：包含首次发送的累计尝试次数，人工重试不清零。
- retry_limit：当前轮次允许达到的累计次数上限；首次为3，人工重试增加3次预算。
- last_attempt_time：最近一次实际开始发送的时间。
- next_retry_time：自动计算的下一次重试时间；默认失败后60秒、300秒。
- sent_time：SMTP确认接受的时间。

认证失败等明确永久错误不自动重试，临时错误按预算重试。人工重试只重置失败收件人的等待状态，已成功收件人不重发。网络超时等不确定结果可能在重试后重复投递；稳定Message-ID用于追踪，不承诺SMTP严格恰好一次。自动重试次数保存在数据库，不依赖进程内计数器。

## 业务调用

调用前需要由宿主恢复可信租户上下文。业务 Service 调用与外层数据库事务一起提交/回滚；后台发送发生在任务提交后，不在业务事务中连接SMTP。

```java
// 站内：保存并提交发布，target也可以指定用户、部门、角色。
Long messageId = messageService.send(new MessageSaveRequest(
        null, null, "系统维护通知", "今晚进行系统维护", "TEXT", "ANNOUNCEMENT",
        new NoticeTarget(true, List.of(), List.of(), List.of(), false),
        "/home", "maintenance", "batch-20261127", "maintenance-message-001", null));

// 邮件：通过配置和模板编码发送，无需业务查询模板内部ID。
Long mailId = mailService.sendTemplate(new MailTemplateSendRequest(
        "company-mail", "approval_result", Map.of("userName", "张三", "result", "已通过"),
        List.of(new MailReceiver(null, "zhangsan@example.com", "张三", Map.of())),
        null, "approval", "batch-20261127", "approval-mail-001", null));
```

示例中的模板变量须与实际保存的模板一致。business_type + business_id用于业务关联，允许一个批次关联多条站内通知和邮件，不加唯一约束。request_id用于同渠道创建幂等，相同请求号必须对应相同请求内容；不同内容拒绝。并发首次创建由唯一索引裁决，冲突请求可按相同request_id重试读取原任务。不要为新的一次提醒复用旧request_id。

业务先发送站内再发送邮件时，可以共用业务类型和批次号，但两个渠道状态独立，不存在“邮件失败撤掉站内消息”的联动。API契约目前是本地Service，不代表已开放跨系统HTTP发送能力。

## 页面与权限

通知中心下包括：通知管理（站内/邮件页签）、邮件配置、邮件模板。铃铛按分类展示未读数量和最近消息，30秒轮询；个人消息中心为登录后的隐藏本地路由 `/notice/inbox`，无需授予后台管理权限。

- notice:message:list/edit/publish/delete：站内管理。
- notice:mail:list/edit/send/delete：邮件管理，send包含失败重试。
- notice:mail-config:list/edit/delete：发件配置。
- notice:mail-template:list/edit/delete/test：模板和测试发送。

所有管理接口同时受租户插件约束。接收人目录只对具备消息/邮件编辑权限的人开放。未安装notice-core的宿主，前端铃铛静默隐藏；正式启用本模块前必须先执行SQL。

## 核验

1. 管理员重新登录，确认新增菜单和按钮，给普通角色授予所需管理权限。
2. 发布站内公告，检查发布状态、接收名单、已读/未读统计及个人铃铛；换用户、换租户验证隔离。
3. 设置计划时间，验证到期前不可见；处理途中停止服务，重启后验证补齐且无重复记录。
4. 配置SMTP和主密钥，创建HTML/文本/Markdown模板，预览并发送到自己的测试邮箱，确认邮件实际显示。
5. 测试失败重试、缺少变量、无效邮箱、停用配置，确认成功收件人不会被再次发送。

自动化测试使用H2的MySQL/PostgreSQL兼容模式验证SQL、事务、租约恢复和重试；这不能代替真实MySQL/PostgreSQL迁移验证，也不能代替真实邮件供应商联调。两万人发布耗时需在部署环境实测，不预设性能承诺。

## 后续扩展

- 短信、钉钉和飞书尚未实现，后续按渠道增加模板、发送内容和接收记录，不把外部手机号或邮箱强制映射成UPM用户。
- 外部HTTP发送入口及API Key认证尚未接入；当前业务通过 `notice-api` 的本地Service契约调用。
- 独立 `notice-start` 在产生单独部署需求时再创建，不提前添加空启动器。
- 早期规划的通用 `notice_template`、`notice_task`、`notice_record` 不再使用，以当前六张业务表、任务状态和租约为准。
